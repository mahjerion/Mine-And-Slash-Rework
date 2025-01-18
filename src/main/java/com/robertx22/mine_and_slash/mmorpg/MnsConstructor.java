package com.robertx22.mine_and_slash.mmorpg;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.ExileCurrencies;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemMods;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqs;
import com.robertx22.library_of_exile.registry.ExileRegistryEventClass;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.OrderedModConstructor;
import com.robertx22.mine_and_slash.aoe_data.GeneratedData;
import com.robertx22.mine_and_slash.aoe_data.database.ailments.Ailments;
import com.robertx22.mine_and_slash.aoe_data.database.boss_spell.BossSpells;
import com.robertx22.mine_and_slash.aoe_data.database.gear_rarities.GearRaritiesAdder;
import com.robertx22.mine_and_slash.aoe_data.database.prophecies.ProphecyStarts;
import com.robertx22.mine_and_slash.database.data.league.LeagueMechanics;
import com.robertx22.mine_and_slash.database.data.loot_chest.base.LootChests;
import com.robertx22.mine_and_slash.database.data.profession.buffs.StatBuffs;
import com.robertx22.mine_and_slash.database.data.stats.types.special.SpecialStats;
import com.robertx22.mine_and_slash.database.registrators.StatsRegister;
import com.robertx22.mine_and_slash.database.registry.ExileDBInit;
import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.SlashDeferred;
import com.robertx22.orbs_of_crafting.main.OrbDatabase;
import com.robertx22.orbs_of_crafting.register.Modifications;
import com.robertx22.orbs_of_crafting.register.Orbs;
import com.robertx22.orbs_of_crafting.register.Requirements;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.Arrays;
import java.util.List;

public class MnsConstructor extends OrderedModConstructor {


    public MnsConstructor(String modid, IEventBus modbus) {
        super(modid, modbus);
    }

    @Override
    public List<ExileRegistryEventClass> getRegisterEvents() {
        return Arrays.asList(
                new GearRaritiesAdder()
        );
    }


    @Override
    public List<ExileKeyHolder> getAllKeyHolders() {
        List<ExileKeyHolder> all = Arrays.asList(
                ItemReqs.INSTANCE,
                ItemMods.INSTANCE,
                ExileCurrencies.INSTANCE,
                // todo split these
                Modifications.INSTANCE,
                Requirements.INSTANCE,
                Orbs.INSTANCE
        );
        return all;
    }

    @Override
    public void registerDeferredContainers(IEventBus bus) {
        SlashDeferred.initContainers(bus);
    }

    @Override
    public void registerDeferredEntries() {
        SlashDeferred.registerEntries();
    }

    @Override
    public void registerDatabases() {
        OrbDatabase.INSTANCE.initDatabases();

        ExileDBInit.initRegistries();
    }


    @Override
    public void registerDatabaseEntries() {
        StatBuffs.load();

        Ailments.init(); // todo will this cause problems. I need to really figure a good way to know when each registry should register
        new StatsRegister().registerAll();

        GeneratedData.addAllObjectsToGenerate();
        BossSpells.init();
        LeagueMechanics.init();
        LootChests.init();
        new ProphecyStarts().registerAll();

        SpecialStats.init();


    }
}
