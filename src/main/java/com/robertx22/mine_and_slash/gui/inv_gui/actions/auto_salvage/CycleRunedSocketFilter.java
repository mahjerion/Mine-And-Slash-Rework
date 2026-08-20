package com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage;

import com.robertx22.mine_and_slash.gui.inv_gui.actions.GuiAction;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.List;

// the minimum socket count a runed base must have to survive auto salvage.
// driven by a plain text button on the salvage screen, this only exists so that button can ride
// the InvGuiPacket -> GuiItemData.onServer -> doAction plumbing instead of needing its own packet
public class CycleRunedSocketFilter extends GuiAction {

    @Override
    public List<Component> getTooltip(Player p) {
        return Arrays.asList(); // the button carries its own tooltip, it isn't an InvGuiButton
    }

    @Override
    public void doAction(Player p, Object data) {
        Load.player(p).config.salvage.cycleRunedMinSockets();
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
        return "salvage_runed_sockets";
    }
}
