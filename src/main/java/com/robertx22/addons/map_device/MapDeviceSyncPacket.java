package com.robertx22.addons.map_device;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.vanilla_mc.packets.proxies.OpenGuiWrapper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: the current state of one map device. Opens the device screen if none is open, or
 * refreshes the one that is. Sent when the block is right-clicked and after every server side change
 * to the device (a slot filled or cleared, a start that was refused).
 */
public class MapDeviceSyncPacket extends MyPacket<MapDeviceSyncPacket> {

    private MapDeviceClientState state = new MapDeviceClientState();

    public MapDeviceSyncPacket() {
    }

    public MapDeviceSyncPacket(MapDeviceClientState state) {
        this.state = state;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "map_device_sync");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        state = MapDeviceClientState.read(tag);
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        state.write(tag);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        // same shape as OpenGuiPacket: the client-only screen code sits behind the wrapper class
        OpenGuiWrapper.openOrRefreshMapDevice(state);
    }

    @Override
    public MyPacket<MapDeviceSyncPacket> newInstance() {
        return new MapDeviceSyncPacket();
    }
}
