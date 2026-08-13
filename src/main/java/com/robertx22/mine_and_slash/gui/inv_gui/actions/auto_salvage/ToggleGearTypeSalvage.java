package com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage;

import com.robertx22.mine_and_slash.database.data.gear_types.bases.BaseGearType;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.GuiAction;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// one cell of the per gear type filter grid. tri state, an unset cell inherits the salvage type's rarity row
public class ToggleGearTypeSalvage extends GuiAction {

    public BaseGearType gearType;
    public GearRarity rarity;

    public ToggleGearTypeSalvage(BaseGearType gearType, GearRarity rarity) {
        this.gearType = gearType;
        this.rarity = rarity;
    }

    private ToggleAutoSalvageRarity.SalvageType parentType() {
        return GearItemData.salvageTypeOf(gearType);
    }

    @Override
    public List<Component> getTooltip(Player p) {
        var list = new ArrayList<Component>();

        var salvage = Load.player(p).config.salvage;
        Optional<Boolean> state = salvage.checkGearTypeSalvageConfig(gearType.GUID(), rarity.GUID());

        if (state.isEmpty()) {
            // show what it currently inherits so the player doesn't have to go back and look
            var inherited = salvage.checkRaritySalvageConfig(parentType(), rarity.GUID())
                    ? Gui.SALVAGE_GT_SALVAGE.locName(gearType.locName(), rarity.locName())
                    : Gui.SALVAGE_GT_KEEP.locName(gearType.locName(), rarity.locName());

            list.add(Gui.SALVAGE_GT_INHERIT.locName(gearType.locName(), rarity.locName(), inherited)
                    .withStyle(ChatFormatting.GRAY));
        } else if (state.get()) {
            list.add(Gui.SALVAGE_GT_SALVAGE.locName(gearType.locName(), rarity.locName()).withStyle(ChatFormatting.GREEN));
        } else {
            list.add(Gui.SALVAGE_GT_KEEP.locName(gearType.locName(), rarity.locName()).withStyle(ChatFormatting.RED));
        }

        list.add(Gui.SALVAGE_GT_CYCLE_TIP.locName().withStyle(ChatFormatting.DARK_GRAY));

        return list;
    }

    @Override
    public ResourceLocation getIcon() {
        return SlashRef.id("textures/gui/inv_gui/icons/" + parentType().name().toLowerCase(Locale.ROOT) + ".png");
    }

    @Override
    public ResourceLocation getBackGroundIcon() {
        return rarity.getGlintTextureFull();
    }

    @Override
    public float[] getIconTint() {
        Player p = ClientOnly.getPlayer();

        if (p == null) {
            return super.getIconTint();
        }

        Optional<Boolean> state = Load.player(p).config.salvage.checkGearTypeSalvageConfig(gearType.GUID(), rarity.GUID());

        if (state.isEmpty()) {
            return new float[]{0.45F, 0.45F, 0.45F, 0.5F}; // inherit, faded out
        }
        if (state.get()) {
            return new float[]{0.35F, 1F, 0.35F, 1F}; // salvage, green
        }
        return new float[]{1F, 0.3F, 0.3F, 1F}; // keep, red
    }

    @Override
    public void doAction(Player p, Object data) {
        Load.player(p).config.salvage.cycleGearTypeSalvageConfig(gearType.GUID(), rarity.GUID());
        Load.player(p).playerDataSync.setDirtyAndSync(p);
    }

    @Override
    public void clientAction(Player p, Object obj) {

    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {

    }

    @Override
    public Object loadExtraData(FriendlyByteBuf buf) {
        return null;
    }

    @Override
    public String GUID() {
        return "gt_sal_" + gearType.GUID() + "_" + rarity.GUID();
    }
}
