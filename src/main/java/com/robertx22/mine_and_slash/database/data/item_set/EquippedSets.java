package com.robertx22.mine_and_slash.database.data.item_set;

import com.robertx22.mine_and_slash.a_libraries.curios.MyCuriosUtils;
import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.saveclasses.unit.GearData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.RepairUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// how many pieces of each set the entity is wearing, and at what average item level.
// the average level is what set bonuses scale their FLAT stats by.
public class EquippedSets {

    public final ItemSet set;
    public final int pieces;
    public final int avgLevel;

    private EquippedSets(ItemSet set, int pieces, int avgLevel) {
        this.set = set;
        this.pieces = pieces;
        this.avgLevel = avgLevel;
    }

    public boolean isActive(SetBonus bonus) {
        return pieces >= bonus.pieces;
    }

    // -- server side: the gear cache already filtered out broken / over level / wrong slot items
    public static List<EquippedSets> of(List<GearData> gears) {
        Counter counter = new Counter();
        for (GearData gear : gears) {
            if (gear.gear != null && gear.stack != null) {
                counter.add(getUniqueId(gear.stack), gear.gear.getLevel());
            }
        }
        return counter.build();
    }

    // -- client side: equipmentCache is never populated on the client (OnEntityTick bails on
    // isClientSide), so the tooltip reads the synced stacks directly and reapplies the same
    // usability filter the server used, otherwise the count shown could disagree with the count
    // that actually granted stats.
    public static List<EquippedSets> of(Player player) {
        Counter counter = new Counter();
        if (player == null) {
            return counter.build();
        }

        var unit = Load.Unit(player);
        if (unit == null) {
            return counter.build();
        }
        int playerLevel = unit.getLevel();

        List<ItemStack> stacks = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            stacks.add(player.getItemBySlot(slot));
        }
        stacks.addAll(MyCuriosUtils.getAllSlots(player));

        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            GearItemData gear = StackSaving.GEARS.loadFrom(stack);
            if (gear == null || !gear.isValidItem()) {
                continue;
            }
            if (gear.getLevel() > playerLevel) {
                continue;
            }
            if (stack.isDamageableItem() && RepairUtils.isItemBroken(stack)) {
                continue;
            }
            counter.add(getUniqueId(stack), gear.getLevel());
        }
        return counter.build();
    }

    /**
     * The same count for a mercenary's stored loadout instead of a player's worn gear.
     * <p>
     * A mercenary's gear lives in {@code MercenaryData.getGear()} and never touches the owner's
     * equipment slots, so {@link #of(Player)} counted the player's own set pieces and printed them
     * on the mercenary's tooltip. The bonus itself was always right - the server path
     * {@link #of(List)} works off the mercenary's own gear like any other entity's - so this only
     * brings the number shown into line with the stats already being granted.
     * <p>
     * Deliberately outside the client cache below, which is keyed on the player's tick and would
     * hand a mercenary's count back to the player's own tooltip on the same frame.
     */
    public static List<EquippedSets> ofMerc(MercenaryData data, int mercLevel) {
        Counter counter = new Counter();
        if (data == null) {
            return counter.build();
        }

        MyInventory inv = data.getGear();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            GearItemData gear = StackSaving.GEARS.loadFrom(stack);
            if (gear == null || !gear.isValidItem()) {
                continue;
            }
            if (gear.getLevel() > mercLevel) {
                continue;
            }
            if (stack.isDamageableItem() && RepairUtils.isItemBroken(stack)) {
                continue;
            }
            counter.add(getUniqueId(stack), gear.getLevel());
        }
        return counter.build();
    }

    public static EquippedSets ofMerc(MercenaryData data, int mercLevel, ItemSet set) {
        for (EquippedSets x : ofMerc(data, mercLevel)) {
            if (x.set.GUID().equals(set.GUID())) {
                return x;
            }
        }
        return new EquippedSets(set, 0, 1);
    }

    // tooltips rebuild every frame; scanning every equipment slot that often is wasteful,
    // so the client side result is memoized for the current tick.
    private static List<EquippedSets> CLIENT_CACHE = null;
    private static int CLIENT_CACHE_TICK = -1;

    public static List<EquippedSets> ofCached(Player player) {
        if (player == null) {
            return new ArrayList<>();
        }
        if (CLIENT_CACHE == null || CLIENT_CACHE_TICK != player.tickCount) {
            CLIENT_CACHE = of(player);
            CLIENT_CACHE_TICK = player.tickCount;
        }
        return CLIENT_CACHE;
    }

    public static EquippedSets of(Player player, ItemSet set) {
        for (EquippedSets x : ofCached(player)) {
            if (x.set.GUID().equals(set.GUID())) {
                return x;
            }
        }
        return new EquippedSets(set, 0, 1);
    }

    public static String getUniqueId(ItemStack stack) {
        CustomItemData custom = StackSaving.CUSTOM_DATA.loadFrom(stack);
        if (custom == null) {
            return "";
        }
        String id = custom.data.get(CustomItemData.KEYS.UNIQUE_ID);
        return id == null ? "" : id;
    }

    // dedupes by unique id, so two copies of the same piece don't complete a set on their own
    private static class Counter {

        // set guid -> (unique guid -> item level)
        private final HashMap<String, HashMap<String, Integer>> found = new HashMap<>();
        private final HashMap<String, ItemSet> sets = new HashMap<>();

        void add(String uniqueId, int lvl) {
            ItemSet set = ItemSet.ofUnique(uniqueId);
            if (set == null) {
                return;
            }
            sets.put(set.GUID(), set);
            found.computeIfAbsent(set.GUID(), x -> new HashMap<>()).putIfAbsent(uniqueId, lvl);
        }

        List<EquippedSets> build() {
            List<EquippedSets> list = new ArrayList<>();
            for (Map.Entry<String, HashMap<String, Integer>> en : found.entrySet()) {
                var pieces = en.getValue();
                if (pieces.isEmpty()) {
                    continue;
                }
                int total = 0;
                for (int lvl : pieces.values()) {
                    total += lvl;
                }
                list.add(new EquippedSets(sets.get(en.getKey()), pieces.size(), total / pieces.size()));
            }
            return list;
        }
    }
}
