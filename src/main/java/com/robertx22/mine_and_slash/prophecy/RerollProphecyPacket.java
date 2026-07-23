package com.robertx22.mine_and_slash.prophecy;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class RerollProphecyPacket extends MyPacket<RerollProphecyPacket> {

    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("reroll_prophecy");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {

    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {

        Load.player(ctx.getPlayer()).prophecy.tryReroll(ctx.getPlayer());

        Load.player(ctx.getPlayer()).playerDataSync.setDirty();
    }

    @Override
    public MyPacket<RerollProphecyPacket> newInstance() {
        return new RerollProphecyPacket();
    }
}
