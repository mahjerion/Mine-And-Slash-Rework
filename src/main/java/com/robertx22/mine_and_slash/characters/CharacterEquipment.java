package com.robertx22.mine_and_slash.characters;

import com.robertx22.mine_and_slash.a_libraries.curios.MyCuriosUtils;
import com.robertx22.mine_and_slash.a_libraries.curios.RefCurios;
import com.robertx22.mine_and_slash.capability.player.helper.GemInventoryHelper;
import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.database.data.stats.types.JewelSocketStat;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// everything a character owns travels with that character. switching moves these stacks off the player
// and into the CharacterData being left behind, then pulls the incoming character's stacks back on.
//
// two kinds of thing move. the worn loadout - armor, offhand, and the ring/necklace/omen curio slots -
// which has to go through the equipment slots and the curio handlers. and the socketed loadout - support
// gems, aura gems and jewels - which is already MyInventory on both sides, so it is a plain slot for slot
// transfer.
//
// the master_bag curio and every third party curio slot are deliberately left alone, as is the inventory
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

    // these mirror the live inventories on PlayerData exactly, so a stash is index for index and the
    // support gem to spell pairing survives - that pairing is positional (hotbar slot i owns gem slots
    // i*6+1 .. i*6+5, see GemInventoryHelper.getHotbarGem) and CharacterData already carries the hotbar.
    public static MyInventory newGemStorage() {
        return new MyInventory(GemInventoryHelper.TOTAL_SLOTS);
    }

    public static MyInventory newAuraStorage() {
        return new MyInventory(GemInventoryHelper.TOTAL_AURAS);
    }

    // the live jewel inventory is sized from JewelSocketStat and resized as that stat moves, so storage
    // is fixed at the stat's hard cap instead - the biggest it can ever legally be.
    public static MyInventory newJewelStorage() {
        return new MyInventory((int) JewelSocketStat.getInstance().max);
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

    // how big a destination has to be to take everything in here. counting items isn't enough, because
    // slots can have gaps - a lone jewel sitting in slot 5 still needs a 6 slot destination.
    private static int requiredSize(MyInventory storage) {
        for (int i = storage.getContainerSize() - 1; i >= 0; i--) {
            if (!storage.getItem(i).isEmpty()) {
                return i + 1;
            }
        }
        return 0;
    }

    // gems, auras and jewels aren't worn - both sides are already a MyInventory, so this is a straight
    // slot for slot move with no equipment slot or curio handler in the way. the source slot is cleared
    // before the stack lands, so a stack is never in two places at once.
    //
    // anything the destination has no room for - a jewel inventory still sized for the outgoing
    // character, a changed GemInventoryHelper constant - stays behind in `from` for the caller to hand
    // back rather than being dropped on the floor.
    private static void moveAll(MyInventory from, MyInventory to) {
        int slots = Math.min(from.getContainerSize(), to.getContainerSize());

        for (int i = 0; i < slots; i++) {
            ItemStack stack = from.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            from.setItem(i, ItemStack.EMPTY);

            if (to.getItem(i).isEmpty()) {
                to.setItem(i, stack);
            } else {
                // the destination should have been emptied by the other half of the swap. it wasn't, so
                // don't overwrite what's there.
                from.setItem(i, stack);
            }
        }
    }

    // move the socketed loadout off the player and into storage, leaving the live inventories empty.
    public static void stashGemsAndJewels(Player player, CharacterData into) {
        try {
            var data = Load.player(player);
            var gems = data.getSkillGemInventory();

            moveAll(gems.getGemsInv(), into.getGems());
            moveAll(gems.getAuraInv(), into.getAuras());
            moveAll(data.jewelData.jewelInventory, into.getJewels());
        } catch (Exception e) {
            e.printStackTrace();
            // whatever made it into storage before the failure has already left the player. hand it
            // straight back rather than leaving items in limbo.
            returnGemsAndJewels(player, into);
        }
    }

    // put a character's stored socketed loadout back on. storage always ends up empty - the stacks land
    // in the live inventories, in the player's inventory, or on the ground, but never in two places.
    public static void restoreGemsAndJewels(Player player, CharacterData from) {
        try {
            var data = Load.player(player);
            var gems = data.getSkillGemInventory();

            // the jewel inventory is sized from JewelSocketStat, which only recomputes on the stat recalc
            // that afterSwap schedules - right now it is still sized for the character we just left. grow
            // it far enough to take what's coming back; JewelData.recalc shrinks it again afterwards and
            // spills anything the incoming character genuinely can't socket.
            data.jewelData.growToAtLeast(requiredSize(from.getJewels()));

            moveAll(from.getGems(), gems.getGemsInv());
            moveAll(from.getAuras(), gems.getAuraInv());
            moveAll(from.getJewels(), data.jewelData.jewelInventory);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // anything still in here had nowhere to go back to - a shrunk jewel inventory, or the restore
        // threw. give it to the player instead of losing it.
        returnGemsAndJewels(player, from);
    }

    public static void returnGemsAndJewels(Player player, CharacterData character) {
        returnAllToPlayer(player, character.getGems());
        returnAllToPlayer(player, character.getAuras());
        returnAllToPlayer(player, character.getJewels());
    }

    public static int countGemsAndJewels(CharacterData character) {
        return countItems(character.getGems())
                + countItems(character.getAuras())
                + countItems(character.getJewels());
    }

    // every inventory a character owns, in one call - used when a character is deleted or reset, where
    // there is nothing to restore into. single entry point so a fourth inventory can't be forgotten at
    // one of those sites later.
    public static void returnEverythingToPlayer(Player player, CharacterData character) {
        returnAllToPlayer(player, character.getEquipment());
        returnGemsAndJewels(player, character);
    }

    public static int countEverything(CharacterData character) {
        return countItems(character.getEquipment()) + countGemsAndJewels(character);
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
        // PlayerData's nbt cache is keyed off this version, and the gem/aura/jewel inventories we just
        // emptied and refilled have no dirty hook of their own - the jewel one is a fresh instance now.
        // ToonActionPacket marks this itself, CreateCharPacket doesn't.
        data.playerDataSync.setDirty();
    }
}
