package com.robertx22.mine_and_slash.aoe_data.database.perks;

import com.robertx22.mine_and_slash.database.OptScaleExactStat;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.CurrencyFind;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.HarvestEventChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.MapFind;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.MobModifierDensity;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ObeliskEventChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.PackSize;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ProphecyCoinFind;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ProphecyEventChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.UberFragmentFind;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.WatcherEyeFind;
import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;

// Flat, player-wide stat perks placed on the Atlas passive tree (data/mmorpg/mmorpg_talent_tree/atlas_passives.json).
// All 4 currently sit directly on the center connector web with no further tree behind them,
// so all 4 are entry perks (allocatable with no prerequisite) - matches the v1 "flat perks only" scope.
public class AtlasPassivePerks {

    public static void init() {

        entry(PerkBuilder.stat("pack_size_node", new OptScaleExactStat(15, PackSize.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("currency_find_node", new OptScaleExactStat(15, CurrencyFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("uber_fragment_find_node", new OptScaleExactStat(10, UberFragmentFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("mob_modifier_density_node", new OptScaleExactStat(10, MobModifierDensity.getInstance(), ModType.FLAT)));

        // find/quantity nodes for probabilistic drops
        entry(PerkBuilder.stat("map_find_node", new OptScaleExactStat(15, MapFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("prophecy_coin_find_node", new OptScaleExactStat(15, ProphecyCoinFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("watcher_eye_find_node", new OptScaleExactStat(10, WatcherEyeFind.getInstance(), ModType.FLAT)));

        // event-chance nodes: raise the chance the league encounter spawns as bonus content in a map
        entry(PerkBuilder.stat("prophecy_event_chance_node", new OptScaleExactStat(25, ProphecyEventChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("harvest_event_chance_node", new OptScaleExactStat(25, HarvestEventChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("obelisk_event_chance_node", new OptScaleExactStat(25, ObeliskEventChance.getInstance(), ModType.FLAT)));
    }

    private static void entry(Perk perk) {
        perk.is_entry = true;
    }
}
