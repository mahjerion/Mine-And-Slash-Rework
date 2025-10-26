package com.robertx22.mine_and_slash.capability.player.helper;

import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class BackpackInventory extends MyInventory {

    Player p;
    Backpacks.BackpackType type;

    public BackpackInventory(Player p, Backpacks.BackpackType type) {
        super(type.getSize());
        this.type = type;
        this.p = p;
    }

    @Override
    public int getMaxStackSize() {
        return super.getMaxStackSize() * type.getStackMultiplier();
    }

    public int getMaxStackSize(ItemStack stack) {
        return stack.getMaxStackSize() * type.getStackMultiplier();
    }

    @Override
    public boolean canAddItem(ItemStack stack) {
        for (int i = 0; i < getContainerSize(); ++i) {
            ItemStack destStack = this.getItem(i);
            if (destStack.isEmpty() || (ItemStack.isSameItemSameTags(destStack, stack) && destStack.getCount() < getMaxStackSize(destStack))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ItemStack addItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            ItemStack copy = stack.copy();
            this.moveItemToOccupiedSlotsWithSameType(copy);
            if (copy.isEmpty()) {
                return ItemStack.EMPTY;
            } else {
                this.moveItemToEmptySlots(copy);
                return copy.isEmpty() ? ItemStack.EMPTY : copy;
            }
        }
    }

    private void moveItemToEmptySlots(ItemStack stack) {
        for(int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack existing = getItem(slot);
            if (existing.isEmpty()) {
                setItem(slot, stack.copyAndClear());
                return;
            }
        }
    }

    private void moveItemToOccupiedSlotsWithSameType(ItemStack stack) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack existing = this.getItem(slot);
            if (ItemStack.isSameItemSameTags(existing, stack)) {
                this.moveItemsBetweenStacks(stack, existing);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
    }

    private void moveItemsBetweenStacks(ItemStack source, ItemStack destination) {
        int maxStack = Math.min(getMaxStackSize(), getMaxStackSize(destination));
        int amount = Math.min(source.getCount(), maxStack - destination.getCount());
        if (amount > 0) {
            destination.grow(amount);
            source.shrink(amount);
            setChanged();
        }
    }
}
