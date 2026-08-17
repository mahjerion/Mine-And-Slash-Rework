package com.robertx22.mine_and_slash.capability.player.helper;

import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class BackpackInventory extends MyInventory {

    Player p;
    Backpacks.BackpackType type;

    public BackpackInventory(Player p, Backpacks.BackpackType type) {
        super(type.getTotalSize());
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

    public BackpackLayouts.Layout getLayout() {
        return BackpackLayouts.get(type);
    }

    // the item this slot is reserved for, or AIR if the slot takes anything the tab accepts
    public Item getReservedItem(int slot) {
        return getLayout().getReservedItem(slot);
    }

    public boolean isReserved(int slot) {
        return getReservedItem(slot) != Items.AIR;
    }

    @Override
    public boolean canAddItem(ItemStack stack) {
        int reserved = getLayout().getSlotFor(stack.getItem());
        if (isSlotUsableBy(reserved, stack)) {
            return true;
        }

        for (int i = 0; i < getContainerSize(); ++i) {
            // a reserved slot belongs to its own item, it's never a fallback for anything else
            if (isReserved(i)) {
                continue;
            }
            if (isSlotUsableBy(i, stack)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSlotUsableBy(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize()) {
            return false;
        }
        ItemStack dest = this.getItem(slot);
        return dest.isEmpty() || (ItemStack.isSameItemSameTags(dest, stack) && dest.getCount() < getMaxStackSize(dest));
    }

    /**
     * Sends the item to its reserved slot before anywhere else, so the tab stays a fixed layout.
     * SimpleContainer's version fills the first empty slot, which would scatter items across the
     * reservations of other currencies.
     */
    @Override
    public ItemStack addItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack left = stack.copy();

        mergeInto(getLayout().getSlotFor(left.getItem()), left);
        if (left.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // top up existing stacks of the same item elsewhere before claiming a new slot
        moveItemToOccupiedSlotsWithSameType(left);
        if (left.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int i = 0; i < getContainerSize(); i++) {
            if (isReserved(i)) {
                continue;
            }
            mergeInto(i, left);
            if (left.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        return left;
    }

    // moves what it can out of stack and into the given slot, shrinking stack as it goes
    private void mergeInto(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize() || stack.isEmpty()) {
            return;
        }
        ItemStack dest = this.getItem(slot);

        if (dest.isEmpty()) {
            this.setItem(slot, stack.split(Math.min(stack.getCount(), getMaxStackSize(stack))));
        } else if (ItemStack.isSameItemSameTags(dest, stack)) {
            moveItemsBetweenStacks(stack, dest);
        }
    }

    /**
     * Re-homes every stack into the slot the current layout reserves for it.
     * <p>
     * Contents are saved by slot index, so a datapack adding or removing a currency shifts every
     * reservation after it and the saved indices no longer line up with the ghost icons. Rather
     * than trying to migrate indices, we just put everything back where it belongs.
     */
    public void resortIntoLayout() {
        BackpackLayouts.Layout layout = getLayout();
        if (layout.slots.isEmpty()) {
            return;
        }

        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = this.getItem(i);
            if (!stack.isEmpty()) {
                contents.add(stack.copy());
            }
            this.setItem(i, ItemStack.EMPTY);
        }

        // reserved slots are filled first so a free-form item can never squat on one
        List<ItemStack> leftovers = new ArrayList<>();
        for (ItemStack stack : contents) {
            mergeInto(layout.getSlotFor(stack.getItem()), stack);
            if (!stack.isEmpty()) {
                leftovers.add(stack);
            }
        }

        for (ItemStack stack : leftovers) {
            ItemStack rest = addItem(stack);
            if (!rest.isEmpty() && p != null && !p.level().isClientSide) {
                // only reachable if the layout shrank. hand it back rather than delete it
                p.getInventory().placeItemBackInInventory(rest);
            }
        }

        setChanged();
    }

    @Override
    protected void moveItemToOccupiedSlotsWithSameType(ItemStack stack) {
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

    @Override
    protected void moveItemsBetweenStacks(ItemStack source, ItemStack destination) {
        int maxStack = Math.min(getMaxStackSize(), getMaxStackSize(destination));
        int amount = Math.min(source.getCount(), maxStack - destination.getCount());
        if (amount > 0) {
            destination.grow(amount);
            source.shrink(amount);
            setChanged();
        }
    }
}
