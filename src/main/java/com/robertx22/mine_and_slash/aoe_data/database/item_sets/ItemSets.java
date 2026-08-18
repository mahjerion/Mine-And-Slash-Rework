package com.robertx22.mine_and_slash.aoe_data.database.item_sets;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;

public class ItemSets implements ExileRegistryInit {

    public static String EMPTY = "empty";

    @Override
    public void registerAll() {
        // the datapack loader throws if a registry ends up empty after loading, so the mod always
        // ships this one placeholder. real sets are expected to come from datapacks.
        ItemSetBuilder.of(EMPTY, "Empty").build();
    }
}
