package com.robertx22.addons.map_device;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Client -> server: the map device screen interactions that aren't an item pick (those go through
 * {@link com.robertx22.mine_and_slash.gui.inv_gui.actions.map_device.MapDeviceEquipAction}): taking an
 * item back out of a slot, pressing the start/join button, and asking for a fresh snapshot.
 */
public class MapDeviceActionPacket extends MyPacket<MapDeviceActionPacket> {

    public enum Action {
        CLEAR_SLOT,
        START_OR_JOIN,
        REQUEST_SYNC
    }

    private BlockPos pos = BlockPos.ZERO;
    private Action action = Action.REQUEST_SYNC;
    private int slot = 0;

    public MapDeviceActionPacket() {
    }

    private MapDeviceActionPacket(BlockPos pos, Action action, int slot) {
        this.pos = pos;
        this.action = action;
        this.slot = slot;
    }

    public static MapDeviceActionPacket clearSlot(BlockPos pos, int slot) {
        return new MapDeviceActionPacket(pos, Action.CLEAR_SLOT, slot);
    }

    public static MapDeviceActionPacket startOrJoin(BlockPos pos) {
        return new MapDeviceActionPacket(pos, Action.START_OR_JOIN, 0);
    }

    public static MapDeviceActionPacket requestSync(BlockPos pos) {
        return new MapDeviceActionPacket(pos, Action.REQUEST_SYNC, 0);
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "map_device_action");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        pos = tag.readBlockPos();
        action = tag.readEnum(Action.class);
        slot = tag.readVarInt();
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeBlockPos(pos);
        tag.writeEnum(action);
        tag.writeVarInt(slot);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Player p = ctx.getPlayer();
        if (p == null) {
            return;
        }
        var device = MapDeviceServer.resolve(p, pos);
        if (device == null) {
            return;
        }

        switch (action) {
            case CLEAR_SLOT -> MapDeviceServer.clearSlot(p, device, pos, slot);
            case START_OR_JOIN -> MapDeviceServer.startOrJoin(p, device, pos);
            case REQUEST_SYNC -> MapDeviceServer.sync(p, device, pos);
        }
    }

    @Override
    public MyPacket<MapDeviceActionPacket> newInstance() {
        return new MapDeviceActionPacket();
    }
}
