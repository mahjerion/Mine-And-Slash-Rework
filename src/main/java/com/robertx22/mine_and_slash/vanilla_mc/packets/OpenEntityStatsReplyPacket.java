package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.UnitNbt;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class OpenEntityStatsReplyPacket extends MyPacket<OpenEntityStatsReplyPacket> {

    public int id;
    public CompoundTag nbt;

    public OpenEntityStatsReplyPacket() {

    }

    public OpenEntityStatsReplyPacket(Entity entity, EntityData data) {
        this.id = entity.getId();
        this.nbt = new CompoundTag();
        UnitNbt.Save(this.nbt, data.getUnit());
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "openentitystatsreply");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        id = tag.readInt();
        nbt = tag.readNbt();
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeInt(id);
        tag.writeNbt(nbt);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Entity entity = ctx.getPlayer().level().getEntity(id);

        if (entity instanceof LivingEntity living) {
            Load.Unit(living).setUnit(UnitNbt.Load(nbt));
            ClientOnly.openEntityStatScreen(living);
        }
    }

    @Override
    public MyPacket<OpenEntityStatsReplyPacket> newInstance() {
        return new OpenEntityStatsReplyPacket();
    }
}
