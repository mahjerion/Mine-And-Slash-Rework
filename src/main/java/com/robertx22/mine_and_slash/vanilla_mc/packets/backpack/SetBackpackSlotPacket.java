package com.robertx22.mine_and_slash.vanilla_mc.packets.backpack;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class SetBackpackSlotPacket extends MyPacket<SetBackpackSlotPacket> {

    // Like ClientboundContainerSetSlotPacket, but sends stack sizes as shorts

    public int containerId;
    public int stateId;
    public int slot;
    public ItemStack itemStack;

    public SetBackpackSlotPacket() {

    }

    public SetBackpackSlotPacket(int containerId, int stateId, int slot, ItemStack itemStack) {
        this.containerId = containerId;
        this.stateId = stateId;
        this.slot = slot;
        this.itemStack = itemStack.copy();
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "setbackpackslot");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        containerId = tag.readUnsignedByte();
        stateId = tag.readVarInt();
        slot = tag.readShort();
        itemStack = BackpackItemSerializer.readItem(tag);
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeByte(containerId);
        tag.writeVarInt(stateId);
        tag.writeShort(slot);
        BackpackItemSerializer.writeItem(tag, itemStack);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        AbstractContainerMenu containerMenu = ClientOnly.getContainerMenu();

        if (containerMenu.containerId == containerId) {
            containerMenu.setItem(slot, stateId, itemStack);
        }
    }

    @Override
    public MyPacket<SetBackpackSlotPacket> newInstance() {
        return new SetBackpackSlotPacket();
    }

}
