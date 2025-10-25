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
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BackpackMenu extends AbstractContainerMenu {

    Player player;
    int size = 6 * 9;
    int containerRows;

    public BackpackMenu(Backpacks.BackpackType type, int pContainerId, Inventory inv) {
        this(type, pContainerId, inv.player, inv, new BackpackInventory(inv.player, type));
    }

    Backpacks.BackpackType type;

    public BackpackMenu(Backpacks.BackpackType type, int pContainerId, Player player, Container playerINV, Container backpackINV) {
        super(SlashContainers.BACKPACK_TABS.get(type).get(), pContainerId);
        this.containerRows = type.rows;
        this.player = player;
        this.size = type.getSize();
        this.type = type;

        try {
            int i = (containerRows - 4) * 18;

            for (int j = 0; j < containerRows; ++j) {
                for (int k = 0; k < 9; ++k) {
                    this.addSlot(new BackpackSlot(backpackINV, k + j * 9, 8 + k * 18, 18 + j * 18));
                }
            }
            for (int l = 0; l < 3; ++l) {
                for (int j1 = 0; j1 < 9; ++j1) {
                    this.addSlot(new Slot(playerINV, j1 + l * 9 + 9, 8 + j1 * 18, 103 + l * 18 + i));
                }
            }

            for (int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(playerINV, i1, 8 + i1 * 18, 161 + i));
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


        return itemstack;
    }

    // Apply stack size multiplier
    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean flag = false;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }

        Slot slot1;
        ItemStack itemstack;
        if (stack.isStackable()) {
            while(!stack.isEmpty()) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                slot1 = (Slot)this.slots.get(i);
                itemstack = slot1.getItem();
                if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemstack)) {
                    int j = itemstack.getCount() + stack.getCount();
                    int maxSize = Math.min(slot1.getMaxStackSize(), stack.getMaxStackSize() * type.stackMultiplier);
                    if (j <= maxSize) {
                        stack.setCount(0);
                        itemstack.setCount(j);
                        slot1.setChanged();
                        flag = true;
                    } else if (itemstack.getCount() < maxSize) {
                        stack.shrink(maxSize - itemstack.getCount());
                        itemstack.setCount(maxSize);
                        slot1.setChanged();
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

        if (!stack.isEmpty()) {
            if (reverseDirection) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while(true) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                slot1 = (Slot)this.slots.get(i);
                itemstack = slot1.getItem();
                if (itemstack.isEmpty() && slot1.mayPlace(stack)) {
                    if (stack.getCount() > slot1.getMaxStackSize()) {
                        slot1.setByPlayer(stack.split(slot1.getMaxStackSize()));
                    } else {
                        slot1.setByPlayer(stack.split(stack.getCount()));
                    }

                    slot1.setChanged();
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
            return Math.min(getMaxStackSize(), stack.getMaxStackSize() * type.stackMultiplier);
        }

        @Override
        public Optional<ItemStack> tryRemove(int count, int decrement, Player player) {
            return super.tryRemove(count, Math.min(decrement, getItem().getMaxStackSize()), player);
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
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
