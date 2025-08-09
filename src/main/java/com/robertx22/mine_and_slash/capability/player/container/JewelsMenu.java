package com.robertx22.mine_and_slash.capability.player.container;

import com.google.common.collect.ImmutableList;
import com.robertx22.mine_and_slash.capability.player.data.JewelData;
import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashContainers;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.vanilla_mc.items.JewelItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class JewelsMenu extends AbstractContainerMenu {
    //176, 166 from AbstractContainerScreen, imageWidth and imageHeight
    public static final Vector2i center = new Vector2i(176 / 2, ((int) (166 * 1.7f / 5)));
    private Container jewelContainer;
    private Player player;
    private List<Vector2i> positions = new ArrayList<>();

    public JewelsMenu(int pContainerId, Inventory pContainer) {
        this(SlashContainers.JEWEL.get(), pContainerId, pContainer, new MyInventory(JewelData.getJewelSocketsMaxStat(ClientOnly.getPlayer())), ClientOnly.getPlayer());
    }

    public List<Vector2i> getPositions() {
        return positions;
    }

    public JewelsMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory pPlayerInventory, Container container, Player player) {
        super(pMenuType, pContainerId);
        this.player = player;
        this.jewelContainer = container;

        int i1;
        int j1;

        //weird code
        List<Vector2i> vector2is = placeJewelSlot(40);
        if (FMLEnvironment.dist == Dist.CLIENT){
            this.positions = vector2is;
        }

        for(i1 = 0; i1 < 3; ++i1) {
            for(j1 = 0; j1 < 9; ++j1) {
                this.addSlot(new Slot(pPlayerInventory, j1 + (i1 + 1) * 9, 8 + j1 * 18, 122 + i1 * 18));
            }
        }

        for(i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(pPlayerInventory, i1, 8 + i1 * 18, 180));
        }
    }

    private List<Vector2i> placeJewelSlot(int radius){
        ImmutableList.Builder<Vector2i> builder = ImmutableList.builder();
        int containerSize = jewelContainer.getContainerSize();


        if (containerSize == 1) {
            addSlot(new Slot(jewelContainer, 0, center.x - 9, center.y - 9){
                @Override
                public boolean mayPlace(ItemStack pStack) {
                    return Load.player(player).jewelData.isWearable(pStack, player);
                }
            });
            return builder.add(new Vector2i(center.x - 9, center.y - 9)).build();
        }

        for (int i = 0; i < containerSize; i++) {
            double angle = 2 * Math.PI * i / containerSize;
            int x = (int) Math.round(center.x + radius * Math.cos(angle)) - 9; // 9 = slot width / 2
            int y = (int) Math.round(center.y + radius * Math.sin(angle)) - 9; // 9 = slot height / 2
            addSlot(new Slot(jewelContainer, i, x, y){
                @Override
                public boolean mayPlace(ItemStack pStack) {
                    return Load.player(player).jewelData.isWearable(pStack, player);
                }
            });
            builder.add(new Vector2i(x, y));
        }
        return builder.build();
    }



    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }



    @Override
    public boolean stillValid(Player pPlayer) {
        return this.jewelContainer.stillValid(pPlayer);
    }
}
