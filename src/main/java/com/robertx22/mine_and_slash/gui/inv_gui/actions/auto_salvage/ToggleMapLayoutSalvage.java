package com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage;

import com.robertx22.dungeon_realm.database.dungeon.Dungeon;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.GuiAction;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.List;

// one map layout crossed off, or not. driven by a text button on the map layouts page, so like
// CycleRunedSocketFilter this only exists to ride the InvGuiPacket -> onServer -> doAction plumbing
public class ToggleMapLayoutSalvage extends GuiAction {

    public Dungeon layout;

    public ToggleMapLayoutSalvage(Dungeon layout) {
        this.layout = layout;
    }

    @Override
    public List<Component> getTooltip(Player p) {
        return Arrays.asList(); // the button carries its own tooltip, it isn't an InvGuiButton
    }

    @Override
    public void doAction(Player p, Object data) {
        Load.player(p).config.salvage.toggleMapLayoutFilter(layout.GUID());
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
        return "map_sal_" + layout.GUID();
    }
}
