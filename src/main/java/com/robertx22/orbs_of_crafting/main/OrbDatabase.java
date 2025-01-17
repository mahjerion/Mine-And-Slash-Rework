package com.robertx22.orbs_of_crafting.main;

import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryContainer;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.SyncTime;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;

public class OrbDatabase {
    public static ExileRegistryType ITEM_MOD = ExileRegistryType.register(SlashRef.MODID, "item_modification", 48, ItemModification.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType ITEM_REQ = ExileRegistryType.register(SlashRef.MODID, "item_requirement", 49, ItemRequirement.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType CURRENCY = ExileRegistryType.register(SlashRef.MODID, "currency", 50, ExileCurrency.SERIALIZER, SyncTime.ON_LOGIN);

    public static void initRegistries() {

        Database.addRegistry(new ExileRegistryContainer<>(OrbDatabase.ITEM_MOD, ""));
        Database.addRegistry(new ExileRegistryContainer<>(OrbDatabase.ITEM_REQ, ""));
        Database.addRegistry(new ExileRegistryContainer<>(OrbDatabase.CURRENCY, "socket_adder"));
    }

    public static void registerObjects() {


    }

    public static ExileRegistryContainer<ExileCurrency> Currency() {
        return Database.getRegistry(CURRENCY);
    }

    public static ExileRegistryContainer<ItemModification> ItemMods() {
        return Database.getRegistry(ITEM_MOD);
    }

    public static ExileRegistryContainer<ItemRequirement> ItemReq() {
        return Database.getRegistry(ITEM_REQ);
    }
}
