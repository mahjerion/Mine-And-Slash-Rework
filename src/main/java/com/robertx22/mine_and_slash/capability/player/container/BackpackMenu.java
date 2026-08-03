package com.robertx22.mine_and_slash.capability.player.container;

import java.util.Optional;

import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.capability.player.helper.BackpackInventory;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashContainers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BackpackMenu extends AbstractContainerMenu {

    Player player;
    Backpacks.BackpackType type;
    int size;
    int containerRows;
    int stackMultiplier;

    public BackpackMenu(Backpacks.BackpackType type, int pContainerId, Inventory inv) {
        this(type, pContainerId, inv.player, inv, new BackpackInventory(inv.player, type));
    }

    public BackpackMenu(Backpacks.BackpackType type, int pContainerId, Player player, Container playerINV, Container backpackINV) {
        super(SlashContainers.BACKPACK_TABS.get(type).get(), pContainerId);
        this.player = player;
        this.type = type;
        this.size = type.getSize();
        this.containerRows = type.getRows();
        this.stackMultiplier = type.getStackMultiplier();

        try {
            int i = (containerRows - 4) * 18;

            for (int j = 0; j < containerRows; ++j) {
                for (int k = 0; k < 9; ++k) {
                    this.addSlot(new BackpackSlot(backpackINV, k + j * 9, 8 + k * 18, 18 + j * 18));
                }
            }
            for (int l = 0; l < 3; ++l) {
                for (int j1 = 0; j1 < 9; ++j1) {
                    this.addSlot(new Slot(playerINV, j1 + l * 9 + 9, 8 + j1 * 18, 104 + l * 18 + i));
                }
            }

            for (int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(playerINV, i1, 8 + i1 * 18, 162 + i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {

        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            if (!type.isValid(itemstack1)) {
                return ItemStack.EMPTY;
            }
            itemstack = itemstack1.copy();
            if (pIndex < size) {
                if (!this.moveItemStackTo(itemstack1, size, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, size, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        // don't loop, we want to limit how many get transferred
        return ItemStack.EMPTY;
    }

    // Apply stack size multiplier
    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean flag = false;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }

        // Only transfer the normal max stack size
        int amountLeftToTake = stack.getMaxStackSize();

        Slot slot;
        ItemStack itemstack;

        if (stack.isStackable()) {
            while (!stack.isEmpty() && amountLeftToTake > 0) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                slot = (Slot)this.slots.get(i);
                itemstack = slot.getItem();
                if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemstack)) {
                    int amountToTake = Math.min(stack.getCount(), amountLeftToTake);
                    int newDestSize = itemstack.getCount() + amountToTake;
                    int maxSize = slot.getMaxStackSize(stack);

                    if (newDestSize <= maxSize) {
                        stack.shrink(amountToTake);
                        itemstack.setCount(newDestSize);
                        slot.setChanged();
                        flag = true;
                        break;
                    } else if (itemstack.getCount() < maxSize) {
                        int amount = maxSize - itemstack.getCount();
                        amountLeftToTake -= amount;
                        stack.shrink(amount);
                        itemstack.setCount(maxSize);
                        slot.setChanged();
                        flag = true;
                    }
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        if (!stack.isEmpty() && amountLeftToTake > 0) {
            if (reverseDirection) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while (true) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                slot = (Slot)this.slots.get(i);
                itemstack = slot.getItem();
                if (itemstack.isEmpty() && slot.mayPlace(stack)) {
                    int amount = Math.min(stack.getCount(), Math.min(slot.getMaxStackSize(), amountLeftToTake));
                    slot.setByPlayer(stack.split(amount));
                    slot.setChanged();
                    flag = true;
                    break;
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return flag;
    }

    // Make right click take only up to 32
    @Override
    protected boolean tryItemClickBehaviourOverride(Player player, ClickAction action, Slot slot, ItemStack clickedItem, ItemStack carriedItem) {
        if (super.tryItemClickBehaviourOverride(player, action, slot, clickedItem, carriedItem)) {
            return true;
        }

        if (!clickedItem.isEmpty() && slot.mayPickup(player) && carriedItem.isEmpty() && action == ClickAction.SECONDARY) {
            int count = (Math.min(clickedItem.getCount(), clickedItem.getMaxStackSize()) + 1) / 2;

            Optional<ItemStack> optional = slot.tryRemove(count, Integer.MAX_VALUE, player);

            optional.ifPresent((taken) -> {
                this.setCarried(taken);
                slot.onTake(player, taken);
            });

            return true;
        }

        return false;
    }

    public class BackpackSlot extends Slot {

        public BackpackSlot(Container pContainer, int pSlot, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return type.isValid(pStack);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(getMaxStackSize(), stack.getMaxStackSize() * stackMultiplier);
        }

        @Override
        public Optional<ItemStack> tryRemove(int count, int decrement, Player player) {
            return super.tryRemove(count, Math.min(decrement, getItem().getMaxStackSize()), player);
        }
    }

    // closes the screen if the backpack item is dropped/given away while it's open
    @Override
    public boolean stillValid(Player pPlayer) {
        return Backpacks.hasBackpack(pPlayer);
    }

    @Override
	public void setSynchronizer(ContainerSynchronizer synchronizer) {
		if (player instanceof ServerPlayer serverPlayer) {
			super.setSynchronizer(new BackpackSynchronizer(serverPlayer));
		} else {
            super.setSynchronizer(synchronizer);
        }
	}
}
