package com.robertx22.mine_and_slash.capability.player.helper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

// had to override tag methods because the simplecontainer doesn't save place in inventory, just autosorts items..
public class MyInventory extends SimpleContainer {


    public MyInventory(int pSize) {
        super(pSize);

    }


    @Override
    public void fromTag(ListTag pContainerNbt) {
        for (int i = 0; i < this.getContainerSize(); ++i) {
            this.setItem(i, ItemStack.EMPTY);
        }

        for (int k = 0; k < pContainerNbt.size(); ++k) {
            CompoundTag compoundtag = pContainerNbt.getCompound(k);
            int j = compoundtag.getByte("Slot") & 255;
            if (j >= 0 && j < this.getContainerSize()) {
                ItemStack stack = ItemStack.of(compoundtag);

                // handle 128+ stack size
                if (compoundtag.contains("IntCount", Tag.TAG_INT)) {
                    stack.setCount(compoundtag.getInt("IntCount"));
                } else if (compoundtag.contains("ShortCount", Tag.TAG_SHORT)) {
                    stack.setCount(compoundtag.getShort("ShortCount"));
                }

                this.setItem(j, stack);
            }
        }

    }

    @Override
    public ListTag createTag() {
        ListTag listtag = new ListTag();

        for (int i = 0; i < this.getContainerSize(); ++i) {
            ItemStack itemstack = this.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte) i);
                itemstack.save(compoundtag);

                if (itemstack.getCount() > 32767) {
                    compoundtag.putInt("IntCount", itemstack.getCount());
                } else if (itemstack.getCount() > 127) {
                    compoundtag.putShort("ShortCount", (short)itemstack.getCount());
                }

                listtag.add(compoundtag);
            }
        }

        return listtag;
    }

    public int getTotalSlots() {
        return this.getContainerSize(); // todo for upgradables maybe limit this
    }

    public boolean hasFreeSlots() {
        return getFreeSlots() > 0;
    }


    public int getFreeSlots() {
        int free = 0;
        for (int i = 0; i < this.getTotalSlots(); i++) {
            ItemStack stack = this.getItem(i);
            if (stack.isEmpty()) {
                free++;
            }
        }
        return free;
    }

}
