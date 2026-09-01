package com.robertx22.mine_and_slash.gui.screens.unique_collection;

import com.robertx22.mine_and_slash.database.data.unique_items.collection.ShardMath;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.vanilla_mc.packets.unique_collection.ClientUniqueCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The four filter dropdowns above the grid.
 * <p>
 * The level and tier brackets are deliberately open ended ("Level 60+") rather than bands. min_drop_lvl
 * and min_tier are minimums - a level 60 unique also drops at 90 - so a closed band would read as a
 * promise the data doesn't make.
 */
public enum BookFilterCategory {

    STATUS(Words.FILTER_STATUS, () -> {
        List<BookFilter> list = new ArrayList<>();
        list.add(new BookFilter(Words.FILTER_STATUS_UNLOCKED.locName(),
                x -> ClientUniqueCollection.isUnlocked(x.GUID())));
        list.add(new BookFilter(Words.FILTER_STATUS_LOCKED.locName(),
                x -> !ClientUniqueCollection.isUnlocked(x.GUID())));
        list.add(new BookFilter(Words.FILTER_STATUS_AFFORDABLE.locName(),
                x -> ClientUniqueCollection.isUnlocked(x.GUID())
                        && ClientUniqueCollection.canAfford(ShardMath.craftCost(x, UniqueCollectionScreen.playerLevel()))));
        return list;
    }),

    SLOT(Words.ON_SLOTS, () -> {
        List<BookFilter> list = new ArrayList<>();
        ExileDB.GearSlots().getList().forEach(slot -> list.add(new BookFilter(
                slot.locName(),
                x -> x.getSlot() != null && x.getSlot().GUID().equals(slot.GUID()))));
        return list;
    }),

    DROP_LEVEL(Words.FILTER_DROP_LEVEL, () -> {
        List<BookFilter> list = new ArrayList<>();
        for (int lvl : new int[]{10, 20, 30, 40, 50, 60, 70, 80, 90}) {
            final int min = lvl;
            list.add(new BookFilter(Words.FILTER_DROP_LEVEL_RANGE.locName(min), x -> x.min_drop_lvl >= min));
        }
        return list;
    }),

    MAP_TIER(Words.FILTER_MAP_TIER, () -> {
        List<BookFilter> list = new ArrayList<>();
        list.add(new BookFilter(Words.FILTER_MAP_TIER_ANY.locName(), x -> x.min_tier <= 0));
        for (int tier : new int[]{1, 20, 40, 60, 80}) {
            final int min = tier;
            list.add(new BookFilter(Words.FILTER_MAP_TIER_RANGE.locName(min), x -> x.min_tier >= min));
        }
        return list;
    });

    public final Words label;
    public final Supplier<List<BookFilter>> options;

    BookFilterCategory(Words label, Supplier<List<BookFilter>> options) {
        this.label = label;
        this.options = options;
    }
}
