package com.robertx22.mine_and_slash.capability.player.container;

import com.google.common.collect.ImmutableList;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashContainers;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class JewelsMenu extends AbstractContainerMenu {
    //176, 166 from AbstractContainerScreen, imageWidth and imageHeight
    public static final Vector2i center = new Vector2i(180 / 2, ((int) (170 * 1.7f / 5)));
    private Container jewelContainer;
    private Player player;
    private List<Vector2i> positions = new ArrayList<>();
    private ContainerSynchronizer synchronizer;

    public JewelsMenu(int pContainerId, Inventory pContainer) {
        this(SlashContainers.JEWEL.get(), pContainerId, pContainer, ClientOnly.getPlayer());
    }

    public JewelsMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory pPlayerInventory, Player player) {
        super(pMenuType, pContainerId);
        this.player = player;
        this.jewelContainer = Load.player(player).jewelData.jewelInventory;

        int i1;
        int j1;


        this.positions = placeJewelSlot(40);

        for (i1 = 0; i1 < 3; ++i1) {
            for (j1 = 0; j1 < 9; ++j1) {
                this.addSlot(new Slot(pPlayerInventory, j1 + (i1 + 1) * 9, 8 + j1 * 18, 122 + i1 * 18));
            }
        }

        for (i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(pPlayerInventory, i1, 8 + i1 * 18, 180));
        }


    }

    public List<Vector2i> getPositions() {
        return positions;
    }

    private List<Vector2i> placeJewelSlot(int radius) {
        ImmutableList.Builder<Vector2i> builder = ImmutableList.builder();
        int containerSize = jewelContainer.getContainerSize();


        if (containerSize == 1) {
            builder.add(addJewelSlot(0, center.x, center.y));
            return builder.build();
        }


        double centerAngle = -Math.PI / 2;
        boolean hasMiddle = (containerSize % 2 == 1);
        int index = 0;


        if (hasMiddle) {
            builder.add(addJewelSlot(index++,
                    center.x + (int) Math.round(radius * Math.cos(centerAngle)),
                    center.y + (int) Math.round(radius * Math.sin(centerAngle))
            ));
        }

        double single = 2 * Math.PI / containerSize;

        for (int i = 0; i < containerSize - (hasMiddle ? 1 : 0); i++) {
            builder.add(addJewelSlot(index++,
                    center.x + (int) Math.round(radius * Math.cos(centerAngle + (i + 1) * single)),
                    center.y + (int) Math.round(radius * Math.sin(centerAngle + (i + 1) * single))
            ));
        }


        return builder.build();
    }

    private Vector2i addJewelSlot(int index, int x, int y) {
        int slotX = x - 9;
        int slotY = y - 9;
        addSlot(new Slot(jewelContainer, index, slotX, slotY) {
            @Override
            public boolean mayPlace(ItemStack pStack) {
                boolean b = Load.player(player).jewelData.isWearable(pStack, player);
                return b;
            }
        });
        return new Vector2i(slotX, slotY);
    }
    

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            if (!Load.player(player).jewelData.isWearable(itemstack1, player)) {
                return ItemStack.EMPTY;
            }
            itemstack = itemstack1.copy();
            int i1 = positions.size() - 1;
            if (i > i1) {
                if (!this.moveItemStackTo(itemstack1, 0, i1 + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, i1 + 1, i1 + 3 * 9, false)) {
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


    @Override
    public boolean stillValid(Player pPlayer) {
        return this.jewelContainer.stillValid(pPlayer);
    }
}
