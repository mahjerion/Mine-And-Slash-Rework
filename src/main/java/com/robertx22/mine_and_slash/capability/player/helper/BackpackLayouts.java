package com.robertx22.mine_and_slash.capability.player.helper;

import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.registry.helpers.ExileCached;
import com.robertx22.mine_and_slash.capability.player.data.Backpacks.BackpackType;
import com.robertx22.mine_and_slash.database.data.gems.Gem;
import com.robertx22.mine_and_slash.database.data.runes.Rune;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.RarityItems;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.vanilla_mc.items.gemrunes.RuneType;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the fixed slot layout of the tabs that use dedicated slots.
 * <p>
 * Every currency/gem/rune gets one reserved slot so the tab reads as a collection, and the
 * client draws a faded ghost icon in the ones you don't own yet. The layout is derived from
 * the loaded registries rather than hardcoded so datapack-added currencies get a slot too,
 * and it is cached until the database changes, same as {@link ExileCurrency#CACHED_MAP}.
 * <p>
 * Both client and server build this independently, so every sort key has to be something both
 * sides agree on. That means registry guids, never translated display names: locname isn't
 * serialized, so datapack entries have an empty one, and it differs per language anyway.
 */
public class BackpackLayouts {

    public static final int COLUMNS = 9;

    // MyInventory writes the slot index as a byte, so a tab can never exceed 256 slots
    public static final int MAX_ROWS = 256 / COLUMNS;

    // free-form rows at the end of the currency tab. crafted infusions (24 of them) live here
    // rather than getting reserved slots, plus headroom for anything else that passes isValid
    private static final int CURRENCY_FREE_ROWS = 4;

    public static class Layout {

        // one entry per slot, in slot order. Items.AIR means "no reservation, accepts anything"
        public final List<Item> slots;
        public final Map<Item, Integer> slotByItem;

        private Layout(List<Item> slots) {
            this.slots = slots;
            this.slotByItem = new HashMap<>();
            for (int i = 0; i < slots.size(); i++) {
                Item item = slots.get(i);
                if (item != Items.AIR) {
                    this.slotByItem.putIfAbsent(item, i);
                }
            }
        }

        public int rows() {
            return slots.size() / COLUMNS;
        }

        public Item getReservedItem(int slot) {
            return slot >= 0 && slot < slots.size() ? slots.get(slot) : Items.AIR;
        }

        // -1 when the item has no reserved slot and belongs in the free-form area
        public int getSlotFor(Item item) {
            return slotByItem.getOrDefault(item, -1);
        }
    }

    private static final Layout EMPTY = new Layout(List.of());

    private static final ExileCached<EnumMap<BackpackType, Layout>> CACHE = new ExileCached<>(() -> {
        EnumMap<BackpackType, Layout> map = new EnumMap<>(BackpackType.class);
        map.put(BackpackType.CURRENCY, buildCurrency());
        map.put(BackpackType.SOCKETABLE, buildSocketable());
        return map;
    }).clearOnDatabaseChange();

    public static boolean usesDedicatedSlots(BackpackType type) {
        return type == BackpackType.CURRENCY || type == BackpackType.SOCKETABLE;
    }

    public static Layout get(BackpackType type) {
        if (!usesDedicatedSlots(type)) {
            return EMPTY;
        }
        try {
            // the client attaches the capability during login, which can be before the registries
            // finish syncing. caching a half-built layout there would stick until a datapack reload
            if (!isDatabaseReady()) {
                CACHE.clear();
                return EMPTY;
            }
            return CACHE.get().getOrDefault(type, EMPTY);
        } catch (Exception e) {
            // the database isn't loaded yet. the tab falls back to a plain free-form grid
            CACHE.clear();
            return EMPTY;
        }
    }

    private static boolean isDatabaseReady() {
        return !LibDatabase.Currency().isEmpty() && !ExileDB.Gems().isEmpty() && !ExileDB.Runes().isEmpty();
    }

    // ------------------------------------------------------------------ currency

    private static Layout buildCurrency() {
        Builder b = new Builder();

        // rarity stones first, ordered by their rarity tier. RARITY_STONE is a HashMap, so the
        // rarity list is what gives a stable order
        List<Item> stones = new ArrayList<>();
        for (String rar : IRarity.NORMAL_GEAR_RARITIES) {
            var reg = RarityItems.RARITY_STONE.get(rar);
            if (reg != null) {
                stones.add(reg.get());
            }
        }
        b.addBlock(stones);

        // then every registered currency, by rarity then alphabetically
        List<ExileCurrency> currencies = new ArrayList<>(LibDatabase.Currency().getList());
        currencies.sort(Comparator
                .comparingInt((ExileCurrency x) -> rarityOrder(x.rar))
                .thenComparing(ExileCurrency::GUID));

        List<Item> orbs = new ArrayList<>();
        for (ExileCurrency cur : currencies) {
            Item item = cur.getItem();
            if (item != null && item != Items.AIR) {
                orbs.add(item);
            }
        }
        b.addBlock(orbs);

        b.addFreeRows(CURRENCY_FREE_ROWS);

        return b.build();
    }

    /**
     * Rarity sort key. ALL_GEAR_RARITIES is the intended display order (common through unique)
     * and GearRarity.item_tier is not usable on its own here - mythic and unique both report 5,
     * and runeword reports 10. Rarities a datapack invented aren't in the list, so they sort
     * into a trailing bucket ordered by their tier.
     */
    private static int rarityOrder(String rarityId) {
        int index = IRarity.ALL_GEAR_RARITIES.indexOf(rarityId);
        if (index >= 0) {
            return index;
        }
        int tier = 0;
        try {
            var rarity = ExileDB.GearRarities().get(rarityId);
            if (rarity != null) {
                tier = rarity.item_tier;
            }
        } catch (Exception e) {
            // unknown rarity, it just lands at the front of the trailing bucket
        }
        return IRarity.ALL_GEAR_RARITIES.size() + tier;
    }

    // --------------------------------------------------------------- socketables

    private static Layout buildSocketable() {
        Builder b = new Builder();

        List<Rune> runes = new ArrayList<>(ExileDB.Runes().getList());

        // common runes first, rarest last. dimension runes are pulled out into their own block
        Comparator<Rune> runeOrder = Comparator
                .comparingInt((Rune x) -> -x.weight)
                .thenComparingInt(x -> x.tier)
                .thenComparing(Rune::GUID);

        List<Rune> normal = new ArrayList<>();
        List<Rune> dimension = new ArrayList<>();
        for (Rune rune : runes) {
            (RuneType.isDimensionRune(rune.GUID()) ? dimension : normal).add(rune);
        }
        normal.sort(runeOrder);
        dimension.sort(Comparator.comparingInt((Rune x) -> x.tier).thenComparing(Rune::GUID));

        b.addBlock(toItems(normal, Rune::getItem));
        b.addBlock(toItems(dimension, Rune::getItem));

        // gems get one row per tier, with the columns being the gem types alphabetically.
        // there are exactly 9 gem types and 9 columns, so this tiles perfectly
        List<Gem> gems = new ArrayList<>(ExileDB.Gems().getList());
        gems.sort(Comparator.comparingInt((Gem x) -> x.tier).thenComparing(x -> x.gem_type));

        List<Gem> currentTier = new ArrayList<>();
        Integer tier = null;
        for (Gem gem : gems) {
            if (tier != null && gem.tier != tier) {
                b.addBlock(toItems(currentTier, Gem::getItem));
                currentTier = new ArrayList<>();
            }
            tier = gem.tier;
            currentTier.add(gem);
        }
        b.addBlock(toItems(currentTier, Gem::getItem));

        return b.build();
    }

    private static <T> List<Item> toItems(List<T> list, java.util.function.Function<T, Item> getter) {
        List<Item> items = new ArrayList<>();
        for (T entry : list) {
            Item item = getter.apply(entry);
            if (item != null && item != Items.AIR) {
                items.add(item);
            }
        }
        return items;
    }

    // ------------------------------------------------------------------- builder

    // lays blocks out one after another, each starting on a fresh row so the groups read cleanly
    private static class Builder {

        private final List<Item> slots = new ArrayList<>();
        private final Set<Item> claimed = new HashSet<>();

        void addBlock(List<Item> items) {
            List<Item> unclaimed = new ArrayList<>();
            for (Item item : items) {
                // datapacks can't register items, so a pack adding a currency has to point it at an
                // item that already exists - often one another currency already uses. only the first
                // entry gets the slot, otherwise the duplicate shows a ghost nothing can ever fill
                if (claimed.add(item)) {
                    unclaimed.add(item);
                }
            }
            if (unclaimed.isEmpty()) {
                return;
            }
            slots.addAll(unclaimed);
            padToRow();
        }

        void addFreeRows(int rows) {
            for (int i = 0; i < rows * COLUMNS; i++) {
                slots.add(Items.AIR);
            }
        }

        private void padToRow() {
            while (slots.size() % COLUMNS != 0) {
                slots.add(Items.AIR);
            }
        }

        Layout build() {
            padToRow();
            if (slots.size() > MAX_ROWS * COLUMNS) {
                System.out.println("[mmorpg] Backpack layout needs " + (slots.size() / COLUMNS)
                        + " rows but a tab can hold at most " + MAX_ROWS
                        + " (the slot index is saved as a byte). Trailing entries lose their reserved slot.");
                return new Layout(new ArrayList<>(slots.subList(0, MAX_ROWS * COLUMNS)));
            }
            return new Layout(slots);
        }
    }
}
