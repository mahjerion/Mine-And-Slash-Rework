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
import java.util.List;

// sits in the last column of the map row, opens the per layout filters
public class OpenMapLayoutFilterAction extends GuiAction {

    @Override
    public List<Component> getTooltip(Player p) {
        var list = new ArrayList<Component>();
        list.add(Gui.SALVAGE_MAP_FILTER_TIP.locName().withStyle(ChatFormatting.GOLD));
        list.add(Gui.SALVAGE_MAP_FILTER_TIP2.locName().withStyle(ChatFormatting.GRAY));
        return list;
    }

    @Override
    public ResourceLocation getIcon() {
        return SlashRef.id("textures/gui/inv_gui/icons/map.png");
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
        ClientOnly.openSalvageMapFilter(0);
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
        return "salvage_map_layouts";
    }
}
