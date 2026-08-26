package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.library_of_exile.utils.RenderUtils;
import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.library_of_exile.wrappers.ExileText;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.gui.buttons.CharacterStatsButtons;
import com.robertx22.mine_and_slash.gui.screens.character_screen.MainHubScreen;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.aoe_data.database.stats.DefenseStats;
import com.robertx22.mine_and_slash.aoe_data.database.stats.OffenseStats;
import com.robertx22.mine_and_slash.aoe_data.database.stats.ResourceStats;
import com.robertx22.mine_and_slash.aoe_data.database.stats.SpellChangeStats;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.effects.defense.MaxElementalResist;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.*;
import com.robertx22.mine_and_slash.database.data.stats.types.generated.ElementalPenetration;
import com.robertx22.mine_and_slash.database.data.stats.types.generated.ElementalResist;
import com.robertx22.mine_and_slash.database.data.stats.types.offense.WeaponDamage;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.health.Health;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.magic_shield.MagicShield;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The two summary icons in the top right: hovering shows a short offence or defence sheet. The design
 * asks for a simplified version of the player's - the full breakdown is one click away on the Stats
 * button.
 */
public class MercInfoButton extends AbstractButton {

    public enum Kind {
        OFFENCE(Words.MercenaryOffence, SlashRef.guiId("stat_groups/damage")),
        DEFENCE(Words.MercenaryDefence, SlashRef.guiId("stat_groups/defense"));

        final Words name;
        final ResourceLocation icon;

        /**
         * Groups rather than one flat list, the same shape as {@link MainHubScreen#STAT_MAP} - a
         * blank line goes between them in the tooltip, so related stats read as a block instead of
         * one undifferentiated wall.
         */
        final List<List<Stat>> stats = new ArrayList<>();

        Kind(Words name, ResourceLocation icon) {
            this.name = name;
            this.icon = icon;
        }
    }

    /**
     * Mirrors {@link MainHubScreen}'s own {@code addTo}: generic on the element type, so a
     * {@code getAll()} or {@code generateAllSingleVariations()} list passes straight in.
     * <p>
     * This is why the groups are built here rather than in the enum constructor arguments -
     * {@code Arrays.asList} can't mix a {@code Stat} with a {@code List<Stat>}, having no common
     * type to infer, and fails to compile rather than nesting.
     */
    private static <T extends Stat> void addTo(Kind kind, List<T> stats) {
        kind.stats.add(stats.stream()
                .map(x -> (Stat) x)
                .collect(Collectors.toList()));
    }

    static {
        addTo(Kind.OFFENCE, Arrays.asList(WeaponDamage.getInstance(), OffenseStats.ACCURACY.get(), ArmorPenetration.getInstance()));
        addTo(Kind.OFFENCE, Arrays.asList(OffenseStats.CRIT_CHANCE.get(), OffenseStats.CRIT_DAMAGE.get()));
        addTo(Kind.OFFENCE, Arrays.asList(SpellChangeStats.SKILL_SPEED.get(), SpellChangeStats.CAST_SPEED.get(),
                SpellChangeStats.ATTACK_CAST_SPEED.get(), SpellChangeStats.COOLDOWN_REDUCTION.get()));
        addTo(Kind.OFFENCE, OffenseStats.ELEMENTAL_DAMAGE.getAll().stream().filter(x -> x.getElement().isValid()).collect(Collectors.toList()));
        addTo(Kind.OFFENCE, OffenseStats.ELEMENTAL_SPELL_DAMAGE.getAll().stream().filter(x -> x.getElement().isValid()).collect(Collectors.toList()));
        addTo(Kind.OFFENCE, new ElementalPenetration(Elements.Elemental).generateAllSingleVariations());

        addTo(Kind.DEFENCE, Arrays.asList(Health.getInstance(), MagicShield.getInstance()));
        addTo(Kind.DEFENCE, Arrays.asList(Armor.getInstance(), DodgeRating.getInstance(), BlockChance.getInstance(), BlockRecovery.getInstance()));
        addTo(Kind.DEFENCE, Arrays.asList(DefenseStats.DAMAGE_REDUCTION.get(), DefenseStats.DAMAGE_RECEIVED.get(), DefenseStats.DAMAGE_REDUCTION_CHANCE.get()));
        addTo(Kind.DEFENCE, new ElementalResist(Elements.Elemental).generateAllSingleVariations());
        addTo(Kind.DEFENCE, new MaxElementalResist(Elements.Elemental).generateAllSingleVariations());
        addTo(Kind.DEFENCE, Arrays.asList(ResourceStats.HEAL_STRENGTH.get(), ResourceStats.HEALING_RECEIVED.get()));
    }

    private final MercenaryScreen screen;
    private final Kind kind;

    private int lastLevel = Integer.MIN_VALUE;
    private boolean lastSummoned;
    private boolean tooltipDirty = true;

    public MercInfoButton(MercenaryScreen screen, Kind kind, int x, int y) {
        super(x, y, MercGui.INFO_ICON_SIZE, MercGui.INFO_ICON_SIZE, Component.empty());
        this.screen = screen;
        this.kind = kind;
    }

    @Override
    public void onPress() {
        // hover only
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        // the mercenary's level is a cheap proxy for "anything about it changed" - every stat here
        // moves with gear or level, and the screen rebuilds on an equip anyway. level alone misses a
        // respawn that lands back on the same level, so summon state is tracked separately - otherwise
        // the "not summoned" tooltip built right after rebuild() never refreshes once the entity arrives.
        int level = screen.getMercLevel();
        if (level != lastLevel) {
            lastLevel = level;
            tooltipDirty = true;
        }
        boolean summoned = screen.isMercSummoned();
        if (summoned != lastSummoned) {
            lastSummoned = summoned;
            tooltipDirty = true;
        }

        gui.setColor(1F, 1F, 1F, 1F);
        gui.blit(MercGui.STAT_ICON_BG, getX(), getY(), 0, 0, width, height, width, height);
        RenderUtils.render16Icon(gui, kind.icon, getX() + 2, getY() + 2);

        if (tooltipDirty) {
            setTooltip(Tooltip.create(TextUTIL.mergeList(buildTooltip())));
            tooltipDirty = false;
        }
    }

    private List<Component> buildTooltip() {
        List<Component> list = new ArrayList<>();
        list.add(kind.name.locName().withStyle(ChatFormatting.GOLD));

        MercenaryEntity merc = screen.getMercEntity();

        if (merc == null) {
            list.add(Words.MercenaryNotSummoned.locName().withStyle(ChatFormatting.RED));
            return list;
        }

        EntityData data = Load.Unit(merc);

        for (List<Stat> group : kind.stats) {
            list.add(ExileText.emptyLine().get());

            for (Stat stat : group) {
                // the hub's own formatter rather than a raw cast. most of the defence list is
                // IUsableStat - armor, dodge, block, the resists - where the calculated number is a
                // rating, not a percentage, so printing it bare read as a wildly wrong value. this
                // prints the effective "35% (420)" the character sheet shows.
                list.add(stat.locName().append(": " + CharacterStatsButtons.getStatString(stat, data)));
            }
        }
        return list;
    }

    @Override
    protected ClientTooltipPositioner createTooltipPositioner() {
        return DefaultTooltipPositioner.INSTANCE;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
