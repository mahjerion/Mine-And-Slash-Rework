package com.robertx22.mine_and_slash.database.data.loot_chest.base;

import com.robertx22.mine_and_slash.database.data.loot_chest.*;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;

public class LootChests {

    public static void init() {

        new CurrencyLootChest().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        new GearLootChest().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        new AuraLootChest().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        new SupportLootChest().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        new GemLootChest().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        new RuneLootChest().registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);

    }
}
