package com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Client -> server: the mercenary screen just opened, send me the master bag contents its equip
 * pickers are allowed to offer. Carries nothing - the reply is the whole point.
 */
public class RequestMercBagPacket extends MyPacket<RequestMercBagPacket> {

    public RequestMercBagPacket() {
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "request_merc_bag");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Player p = ctx.getPlayer();
        if (p == null) {
            return;
        }
        // same gate the rest of the mercenary packets use - nothing to offer a character that hasn't
        // unlocked mercenaries yet
        if (!MercenaryManager.isUnlocked(p)) {
            return;
        }
        MercBagServer.sync(p);
    }

    @Override
    public MyPacket<RequestMercBagPacket> newInstance() {
        return new RequestMercBagPacket();
    }
}
