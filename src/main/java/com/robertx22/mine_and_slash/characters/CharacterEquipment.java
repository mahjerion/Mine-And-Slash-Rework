package com.robertx22.mine_and_slash.characters;

import com.robertx22.mine_and_slash.a_libraries.curios.MyCuriosUtils;
import com.robertx22.mine_and_slash.a_libraries.curios.RefCurios;
import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// the gear a character wears travels with that character. switching moves these stacks off the player
// and into the CharacterData being left behind, then pulls the incoming character's stacks back on.
//
// only the RPG loadout is touched: armor, offhand, and the ring/necklace/omen curio slots. the
// master_bag curio and every third party curio slot are deliberately left alone, as is the inventory
// and the mainhand.
public class CharacterEquipment {

    // armor and offhand take the first indices, then a fixed block per curio slot type.
    private static final List<EquipmentSlot> VANILLA_SLOTS = Arrays.asList(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND);

    // each curio slot type owns a fixed index range, rather than being packed in the order the handlers
    // happen to come back in. if a slot type is missing on one side of a switch - a datapack change, or
    // curios not answering - the remaining types still land where they were stored instead of shifting up.
    //
    // NOTE: do not use MyCuriosUtils.SLOTS here. it lists "backpack", which isn't the registered slot
    // name (that's master_bag, see RefCurios), and we don't want the bag travelling between characters.
    private record CurioBlock(String slot, int base, int count) {
    }

    private static final List<CurioBlock> CURIO_BLOCKS = Arrays.asList(
            new CurioBlock(RefCurios.RING, 5, 2),
            new CurioBlock(RefCurios.NECKLACE, 7, 1),
            new CurioBlock(RefCurios.OMEN, 8, 1));

    public static final int SIZE = 9; // 4 armor + offhand + 2 rings + necklace + omen

    public static MyInventory newStorage() {
        return new MyInventory(SIZE);
    }

    private static ICurioStacksHandler handlerFor(Player player, CurioBlock block) {
        List<ICurioStacksHandler> found = MyCuriosUtils.getHandlers(Collections.singletonList(block.slot()), player);
        return found.isEmpty() ? null : found.get(0);
    }

    // move everything the player is wearing into storage, leaving the slots empty.
    public static void stashInto(Player player, MyInventory storage) {
        try {
            for (int i = 0; i < VANILLA_SLOTS.size(); i++) {
                EquipmentSlot slot = VANILLA_SLOTS.get(i);
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                storage.setItem(i, stack.copy());
                player.setItemSlot(slot, ItemStack.EMPTY);
            }

            for (CurioBlock block : CURIO_BLOCKS) {
                ICurioStacksHandler handler = handlerFor(player, block);
                if (handler == null) {
                    continue;
                }
                // a datapack can grow a slot type past the room we reserved for it. the extra slots
                // simply don't travel with the character - better than silently eating what's in them.
                int slots = Math.min(handler.getSlots(), block.count());
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = handler.getStacks().getStackInSlot(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    storage.setItem(block.base() + i, stack.copy());
                    handler.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // whatever made it into storage before the failure has already left the player. hand it
            // straight back rather than leaving items in limbo.
            returnAllToPlayer(player, storage);
        }
    }

    // put a character's stored gear back on. storage always ends up empty - the stacks land on the
    // player, in their inventory, or on the ground, but never in two places at once.
    public static void restoreFrom(Player player, MyInventory storage) {
        try {
            for (int i = 0; i < VANILLA_SLOTS.size(); i++) {
                ItemStack stack = storage.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                storage.setItem(i, ItemStack.EMPTY);

                EquipmentSlot slot = VANILLA_SLOTS.get(i);
                if (player.getItemBySlot(slot).isEmpty()) {
                    player.setItemSlot(slot, stack);
                } else {
                    PlayerUtils.forceUnequipItem(stack, player);
                }
            }

            for (CurioBlock block : CURIO_BLOCKS) {
                ICurioStacksHandler handler = handlerFor(player, block);
                if (handler == null) {
                    continue;
                }
                int slots = Math.min(handler.getSlots(), block.count());
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = storage.getItem(block.base() + i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    storage.setItem(block.base() + i, ItemStack.EMPTY);

                    if (handler.getStacks().getStackInSlot(i).isEmpty()) {
                        handler.getStacks().setStackInSlot(i, stack);
                    } else {
                        PlayerUtils.forceUnequipItem(stack, player);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // anything still in here had nowhere to go back to - a datapack shrank the ring slots, curios
        // wasn't available, or the restore threw. give it to the player instead of losing it.
        returnAllToPlayer(player, storage);
    }

    // hand every stored stack back and empty the storage. used when a character is deleted or reset,
    // where there is no loadout to restore into.
    public static void returnAllToPlayer(Player player, MyInventory storage) {
        if (storage == null) {
            return;
        }
        for (int i = 0; i < storage.getContainerSize(); i++) {
            ItemStack stack = storage.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            storage.setItem(i, ItemStack.EMPTY);
            PlayerUtils.forceUnequipItem(stack, player);
        }
    }

    public static int countItems(MyInventory storage) {
        if (storage == null) {
            return 0;
        }
        return storage.getContainerSize() - storage.getFreeSlots();
    }

    // writing to the armor slots and to the curio stack handlers directly doesn't reliably fire
    // CurioChangeEvent, which is what normally tells the mod the loadout changed. invalidate by hand.
    //
    // deliberately no capAll() here - the stat recalc these flags schedule hasn't run yet, so capping
    // now would clamp resources against the outgoing character's maximums. CachedPlayerStats.ALLOCATED
    // caps once its own recalc has happened.
    public static void afterSwap(Player player) {
        var data = Load.player(player);

        Load.Unit(player).setEquipsChanged();
        data.cachedStats.setAllDirty();
        // omensFilled is a cached int on PlayerData, not per character, and it drives omen set bonuses
        data.recalcOmensFilled();
    }
}
