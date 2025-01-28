package com.robertx22.mine_and_slash.a_libraries.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MyCurioUtils {

    public static List<String> SLOTS = Arrays.asList("ring", "necklace", "omen", "backpack");

    public static List<ICurioStacksHandler> getHandlers(Player player) {
        return getHandlers(SLOTS, player);
    }

    public static List<ICurioStacksHandler> getHandlers(List<String> slots, Player player) {

        List<ICurioStacksHandler> list = new ArrayList<>();

        ICuriosItemHandler handler = CuriosApi.getCuriosHelper()
                .getCuriosHandler(player)
                .orElse(null);


        if (handler != null) {
            for (String slot : slots) {

                Optional<ICurioStacksHandler> sh = handler.getStacksHandler(slot);

                if (sh.isPresent()) {
                    list.add(sh.get());
                }

            }
        }
        return list;
    }

    public static List<ItemStack> getAllSlots(Player player) {
        ICuriosItemHandler handler = CuriosApi.getCuriosHelper()
                .getCuriosHandler(player)
                .orElse(null);

        if (handler != null) {
            var slots = handler.getCurios().entrySet().stream().map(x -> x.getKey()).collect(Collectors.toList());
            return getAllSlots(slots, player);
        }
        return Arrays.asList();
    }

    public static ItemStack get(String slot, Player player, int num) {

        List<ItemStack> list = getAllSlots(Arrays.asList(slot), player);

        if (num + 1 > list.size()) {
            return ItemStack.EMPTY;
        } else {
            return list.get(num);
        }
    }

    public static List<ItemStack> getAllSlots(List<String> slots, Player player) {

        List<ItemStack> list = new ArrayList<>();

        getHandlers(slots, player).forEach(x -> {
            for (int i = 0; i < x
                    .getSlots(); i++) {

                ItemStack stack = x
                        .getStacks()
                        .getStackInSlot(i);
                // if (!stack.isEmpty()) {
                list.add(stack);
                // }
            }
        });

        return list;

    }
}
