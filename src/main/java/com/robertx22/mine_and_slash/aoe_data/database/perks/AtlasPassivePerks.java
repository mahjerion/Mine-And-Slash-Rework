package com.robertx22.mine_and_slash.aoe_data.database.perks;

import com.robertx22.mine_and_slash.database.OptScaleExactStat;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.CurrencyFind;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.MobModifierDensity;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.PackSize;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.UberFragmentFind;
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
    }

    private static void entry(Perk perk) {
        perk.is_entry = true;
    }
}
