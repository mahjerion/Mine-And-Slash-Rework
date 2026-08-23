package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SummonPetAction;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class UnsummonPacket extends MyPacket<UnsummonPacket> {
    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("unsummon");
    }

    @Override
    public void loadFromData(FriendlyByteBuf friendlyByteBuf) {

    }

    @Override
    public void saveToData(FriendlyByteBuf friendlyByteBuf) {

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        SummonPetAction.despawnAllSummons(ctx.getPlayer());
    }

    @Override
    public MyPacket<UnsummonPacket> newInstance() {
        return new UnsummonPacket();
    }
}
