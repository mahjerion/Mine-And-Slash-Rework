package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.RenderUtils;
import com.robertx22.library_of_exile.utils.TextUTIL;
import com.robertx22.mine_and_slash.database.data.mercenary.ClientMercenary;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.gui.inv_gui.GuiInventoryGrids;
import com.robertx22.mine_and_slash.gui.inv_gui.InvGuiScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenaryActionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * One of the mercenary's 4 active skill slots. The slot order is the cast priority queue, so shift
 * clicking moves a skill earlier or later in it.
 */
public class MercSkillButton extends AbstractButton {

    private static final ResourceLocation EMPTY = SlashRef.guiId("empty_spell");

    private final MercenaryScreen screen;
    private final int slot;


    public MercSkillButton(MercenaryScreen screen, int slot, int x, int y, int size) {
        super(x, y, size, size, Component.empty());
        this.screen = screen;
        this.slot = slot;
    }

    private Spell getSpell() {
        MercenaryData data = screen.getMercData();
        return data == null ? null : data.getEquippedSpell(slot);
    }

    @Override
    public void onPress() {
        // cancelling the pick returns to the mercenary screen, matching where MercPickSkillAction
        // leaves you after a successful one
        Minecraft.getInstance().setScreen(new InvGuiScreen(
                GuiInventoryGrids.ofMercSkillChoices(ClientOnly.getPlayer(), slot),
                Words.Mercenary.locName(), () -> ClientOnly.setScreen(new MercenaryScreen())));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!this.isMouseOver(mx, my) || !this.active) {
            return super.mouseClicked(mx, my, button);
        }
        if (button == 1) {
            // right click clears the slot
            Packets.sendToServer(MercenaryActionPacket.setSkill(slot, ""));
            return true;
        }
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            // shift click walks it up the queue, wrapping at the front so one modifier is enough
            Packets.sendToServer(MercenaryActionPacket.movePriority(slot, slot > 0));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        Spell spell = getSpell();

        gui.setColor(1F, 1F, 1F, 1F);
        gui.blit(spell == null ? EMPTY : spell.getIconLoc(), getX(), getY(), 0, 0, width, height, width, height);

        // rebuilt per frame while hovered rather than cached, the same way the player's SpellButton and
        // InvGuiButton do it - shift and alt expand the spell tooltip, and a cached one never sees the
        // key go down. it also picks up the mercenary being summoned or re-geared while the screen is open
        if (isHovered()) {
            setTooltip(Tooltip.create(TextUTIL.mergeList(buildTooltip(spell))));
        }
    }

    private List<Component> buildTooltip(Spell spell) {
        List<Component> list = new ArrayList<>();

        if (spell == null) {
            list.add(Words.MercenaryEmptySkill.locName().withStyle(ChatFormatting.GRAY));
            list.add(Words.MercenaryPickForSlot.locName().withStyle(ChatFormatting.DARK_GRAY));
            return list;
        }
        list.addAll(spell.GetTooltipString(ClientMercenary.tooltipInfo()));
        if (!ClientMercenary.isOut()) {
            // the numbers above fell back to the player's stats, so say why they can be off
            list.add(Words.MercenaryNotSummoned.locName().withStyle(ChatFormatting.RED));
        }
        list.add(Words.MercenaryPriorityTip.locName().withStyle(ChatFormatting.DARK_GRAY));
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
