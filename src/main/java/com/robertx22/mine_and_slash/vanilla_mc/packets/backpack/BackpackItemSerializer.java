package com.robertx22.mine_and_slash.vanilla_mc.packets.backpack;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BackpackItemSerializer {
    public static void writeItem(FriendlyByteBuf buf, ItemStack stack) {
        writeItemStack(buf, stack, true);
    }

    public static void writeItemStack(FriendlyByteBuf buf, ItemStack stack, boolean limitedTag) {
        if (stack.isEmpty()) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            Item item = stack.getItem();
            buf.writeId(BuiltInRegistries.ITEM, item);
            buf.writeVarInt(stack.getCount());
            CompoundTag compoundtag = null;
            if (item.isDamageable(stack) || item.shouldOverrideMultiplayerNbt()) {
                compoundtag = limitedTag ? stack.getShareTag() : stack.getTag();
            }
            buf.writeNbt(compoundtag);
        }
    }

    public static ItemStack readItem(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return ItemStack.EMPTY;
        } else {
            Item item = (Item)buf.readById(BuiltInRegistries.ITEM);
            int count = buf.readVarInt();
            ItemStack itemstack = new ItemStack(item, count);
            itemstack.readShareTag(buf.readNbt());
            return itemstack;
        }
    }
}
