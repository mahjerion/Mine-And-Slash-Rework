package com.robertx22.mine_and_slash.database.data.auto_item;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.registry.helpers.ExileCached;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.ExileStacklessData;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoItem implements JsonExileRegistry<AutoItem>, IAutoGson<AutoItem> {
    public static AutoItem SERIALIZER = new AutoItem();

    public String id = "";
    public int weight = 1000;

    public String item_id = "";

    public String custom_item_generation = "";


    public static void of(String id, String itemid, String gen) {
        AutoItem b = new AutoItem();
        b.item_id = itemid;
        b.id = id;
        b.custom_item_generation = gen;

        b.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
    }

    public static void tryInsertTo(ItemStack stack, Player p) {
        ItemStack fixed = enforce(stack, p);
        if (fixed != null) {
            stack.setTag(fixed.getTag()); // todo this needs rework after 1.21
        }
    }

    public static List<AutoItem> getAllFor(Item item) {
        var list = CACHED_MAP.get().get(item);
        return list == null ? Collections.emptyList() : list;
    }

    // the unique ids the generations for this item pin it to. empty means no generation forces a unique,
    // in which case any gear data on the stack is legal.
    public static Set<String> getForcedUniqueIds(Item item) {
        Set<String> ids = new HashSet<>();
        for (AutoItem auto : getAllFor(item)) {
            var gen = ExileDB.CustomItemGenerations().get(auto.custom_item_generation);
            if (gen != null && !gen.uniq_id.isEmpty()) {
                ids.add(gen.uniq_id);
            }
        }
        return ids;
    }

    /**
     * The single source of truth for "is this stack what the datapack says it must be".
     * Returns null if the stack is already fine, otherwise the corrected stack. Never mutates the input.
     * Server side only - the AUTO_ITEM/CUSTOM_ITEM registries are SyncTime.NEVER so this no-ops on clients.
     */
    public static ItemStack enforce(ItemStack stack, Player p) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (getAllFor(stack.getItem()).isEmpty()) {
            return null; // not an auto managed item, the common case
        }
        if (stack.getCount() != 1) {
            return null; // gear nbt on a multi count stack is meaningless
        }

        if (!StackSaving.GEARS.has(stack)) {
            // soulless. this is a fresh vanilla item that has to become gear
            if (stack.hasTag() && stack.getTag().getBoolean("free_souled")) {
                return null; // already processed once and legitimately left without gear data
            }
            return generateOn(stack, p);
        }

        var forced = getForcedUniqueIds(stack.getItem());
        if (forced.isEmpty()) {
            return null; // no generation pins a unique, so whatever gear it has is legal
        }
        if (forced.contains(getUniqueIdOf(stack))) {
            return null; // legitimately obtained
        }

        // gear data that the datapack never allowed - regenerate from a clean stack so no leftover
        // potential/soul/crafted/corruption data from the exploit survives
        ItemStack fixed = generateOn(new ItemStack(stack.getItem()), p);

        if (fixed == null || !forced.contains(getUniqueIdOf(fixed))) {
            // the generation couldn't produce the unique it promises (bad uniq_id, unique filtered out
            // of the pool). handing this back would reroll the item every single sweep, forever.
            if (failedItems.add(stack.getItem())) {
                ExileLog.get().warn("Auto item " + stack.getItem() + " has illegal gear data but its custom item "
                        + "generation can't produce the unique it declares - leaving the item alone.");
            }
            return null;
        }
        return fixed;
    }

    private static final Set<Item> failedItems = new HashSet<>();

    private static String getUniqueIdOf(ItemStack stack) {
        CustomItemData custom = StackSaving.CUSTOM_DATA.loadFrom(stack);
        return custom == null ? "" : custom.data.get(CustomItemData.KEYS.UNIQUE_ID);
    }

    private static ItemStack generateOn(ItemStack stack, Player p) {
        var auto = getRandom(stack.getItem());
        if (auto == null) {
            return null;
        }
        var ex = ExileStack.of(stack);
        ex.getStack().getOrCreateTag().putBoolean("free_souled", true);
        auto.create(p).apply(ex);
        return ex.getStack();
    }

    public ExileStacklessData create(Player p) {
        return ExileDB.CustomItemGenerations().get(custom_item_generation).create(p);
    }

    public static ExileCached<HashMap<Item, List<AutoItem>>> CACHED_MAP = new ExileCached<>(() -> {
        HashMap<Item, List<AutoItem>> map = new HashMap<>();

        for (AutoItem auto : ExileDB.AutoItems().getList()) {
            var item = VanillaUTIL.REGISTRY.items().get(new ResourceLocation(auto.item_id));
            if (item != Items.AIR) {
                if (!map.containsKey(item)) {
                    map.put(item, new ArrayList<>());
                }
                map.get(item).add(auto);
            }
        }
        return map;
    }).clearOnDatabaseChange();


    public static AutoItem getRandom(Item item) {
        var list = getAllFor(item);
        if (!list.isEmpty()) {
            return RandomUtils.weightedRandom(list);
        }
        return null;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.AUTO_ITEM;
    }

    @Override
    public Class<AutoItem> getClassForSerialization() {
        return AutoItem.class;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }
}
