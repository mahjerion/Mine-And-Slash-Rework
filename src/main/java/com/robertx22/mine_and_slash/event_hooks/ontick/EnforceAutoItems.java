package com.robertx22.mine_and_slash.event_hooks.ontick;

import com.robertx22.mine_and_slash.a_libraries.curios.MyCuriosUtils;
import com.robertx22.mine_and_slash.database.data.auto_item.AutoItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * AutoItem conversion used to happen only on ItemCraftedEvent and EntityItemPickupEvent, which meant
 * an item that entered the inventory any other way (shift clicked out of a chest, sucked up by a
 * backpack pickup upgrade, /give, trades) stayed a plain vanilla item and dodged the unique/level gate.
 * <p>
 * This sweep is the actual chokepoint. It doesn't care how the item arrived, and it also repairs items
 * that were already exploited before this check existed.
 */
public class EnforceAutoItems {

    public static void check(Player player) {

        var inv = player.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) { // main + armor + offhand
            ItemStack fixed = AutoItem.enforce(inv.getItem(i), player);
            if (fixed != null) {
                inv.setItem(i, fixed);
            }
        }

        for (ICurioStacksHandler handler : MyCuriosUtils.getAllHandlers(player)) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack fixed = AutoItem.enforce(handler.getStacks().getStackInSlot(i), player);
                if (fixed != null) {
                    handler.getStacks().setStackInSlot(i, fixed);
                }
            }
        }
    }
}
