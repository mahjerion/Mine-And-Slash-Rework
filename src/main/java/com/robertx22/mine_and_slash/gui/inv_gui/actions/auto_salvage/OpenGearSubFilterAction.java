package com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage;

import com.robertx22.mine_and_slash.gui.inv_gui.actions.GuiAction;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// sits in the last column of a salvage type row, opens that type's per gear type filters
public class OpenGearSubFilterAction extends GuiAction {

    // the only salvage types backed by gear types
    public static List<ToggleAutoSalvageRarity.SalvageType> TYPES = Arrays.asList(
            ToggleAutoSalvageRarity.SalvageType.WEAPON,
            ToggleAutoSalvageRarity.SalvageType.ARMOR,
            ToggleAutoSalvageRarity.SalvageType.GEAR
    );

    public ToggleAutoSalvageRarity.SalvageType type;

    public OpenGearSubFilterAction(ToggleAutoSalvageRarity.SalvageType type) {
        this.type = type;
    }

    @Override
    public List<Component> getTooltip(Player p) {
        var list = new ArrayList<Component>();
        list.add(Gui.SALVAGE_SUB_FILTER_TIP.locName(type.word.locName()).withStyle(ChatFormatting.GOLD));
        list.add(Gui.SALVAGE_SUB_FILTER_TIP2.locName().withStyle(ChatFormatting.GRAY));
        return list;
    }

    @Override
    public ResourceLocation getIcon() {
        return SlashRef.id("textures/gui/inv_gui/icons/" + type.name().toLowerCase(Locale.ROOT) + ".png");
    }

    @Override
    public boolean isClientOnly() {
        return true;
    }

    @Override
    public void doAction(Player p, Object data) {

    }

    @Override
    public void clientAction(Player p, Object obj) {
        ClientOnly.openSalvageSubFilter(type, 0);
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
        return "salvage_sub_" + type.name().toLowerCase(Locale.ROOT);
    }
}
