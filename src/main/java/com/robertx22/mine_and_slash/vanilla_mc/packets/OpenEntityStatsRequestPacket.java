package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.UnitNbt;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class OpenEntityStatsRequestPacket extends MyPacket<OpenEntityStatsRequestPacket> {

    public int id;

    public OpenEntityStatsRequestPacket() {

    }

    public OpenEntityStatsRequestPacket(Entity entity) {
        this.id = entity.getId();

    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "openentitystatsrequest");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        id = tag.readInt();
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeInt(id);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Entity entity = ctx.getPlayer().level().getEntity(id);

        if (entity instanceof LivingEntity) {
            Packets.sendToClient(ctx.getPlayer(), new OpenEntityStatsReplyPacket(entity, Load.Unit(entity)));
        }
    }

    @Override
    public MyPacket<OpenEntityStatsRequestPacket> newInstance() {
        return new OpenEntityStatsRequestPacket();
    }
}
