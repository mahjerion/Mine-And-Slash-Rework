package com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.vanilla_mc.packets.proxies.OpenGuiWrapper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: the master bag contents the mercenary equip picker may offer. Sent when the
 * mercenary screen asks for it, and again after every change to a mercenary loadout so the counts
 * stay right.
 */
public class MercBagSyncPacket extends MyPacket<MercBagSyncPacket> {

    private MercBagClientState state = new MercBagClientState();

    public MercBagSyncPacket() {
    }

    public MercBagSyncPacket(MercBagClientState state) {
        this.state = state;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "merc_bag_sync");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        state = MercBagClientState.read(tag);
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        state.write(tag);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        // same shape as MapDeviceSyncPacket: the client-only screen code sits behind the wrapper class
        OpenGuiWrapper.onMercBagSync(state);
    }

    @Override
    public MyPacket<MercBagSyncPacket> newInstance() {
        return new MercBagSyncPacket();
    }
}
