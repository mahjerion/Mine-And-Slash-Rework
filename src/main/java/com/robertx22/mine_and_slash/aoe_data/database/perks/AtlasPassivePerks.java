package com.robertx22.mine_and_slash.aoe_data.database.perks;

import com.robertx22.mine_and_slash.database.OptScaleExactStat;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.*;
import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;

// Flat, player-wide stat perks placed on the Atlas passive tree (data/mmorpg/mmorpg_talent_tree/atlas_passives.json).
// All 4 currently sit directly on the center connector web with no further tree behind them,
// so all 4 are entry perks (allocatable with no prerequisite) - matches the v1 "flat perks only" scope.
public class AtlasPassivePerks {

    public static void init() {

        entry(PerkBuilder.stat("pack_size_node", new OptScaleExactStat(4, PackSize.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("pack_size_node_big", new OptScaleExactStat(10, PackSize.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("mob_modifier_density_node", new OptScaleExactStat(3, MobModifierDensity.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("mob_modifier_density_node_big", new OptScaleExactStat(8, MobModifierDensity.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("currency_find_node", new OptScaleExactStat(2, CurrencyFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("currency_find_node_big", new OptScaleExactStat(5, CurrencyFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("omen_find_node", new OptScaleExactStat(2, OmenFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("omen_find_node_big", new OptScaleExactStat(5, OmenFind.getInstance(), ModType.FLAT)));

        // loot quantity/quality nodes (stats already exist + are consumed in LootInfo/rarity rolls)
        entry(PerkBuilder.stat("item_find_node", new OptScaleExactStat(1, TreasureQuantity.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("item_find_node_big", new OptScaleExactStat(3, TreasureQuantity.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("magic_find_node", new OptScaleExactStat(1, TreasureQuality.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("magic_find_node_big", new OptScaleExactStat(3, TreasureQuality.getInstance(), ModType.FLAT)));

        // gem find nodes: socketable gems (GemLootGen) and skill gems (Aura/Supp gem gens)
        entry(PerkBuilder.stat("gem_find_node", new OptScaleExactStat(2, GemFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("gem_find_node_big", new OptScaleExactStat(5, GemFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("skill_gem_find_node", new OptScaleExactStat(2, SkillGemFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("skill_gem_find_node_big", new OptScaleExactStat(5, SkillGemFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("rune_find_node", new OptScaleExactStat(2, RuneFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("rune_find_node_big", new OptScaleExactStat(5, RuneFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("jewel_find_node", new OptScaleExactStat(2, JewelFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("jewel_find_node_big", new OptScaleExactStat(5, JewelFind.getInstance(), ModType.FLAT)));

        // find/quantity nodes for probabilistic drops
        entry(PerkBuilder.stat("uber_fragment_find_node", new OptScaleExactStat(3, UberFragmentFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("uber_fragment_find_node_big", new OptScaleExactStat(8, UberFragmentFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("map_find_node", new OptScaleExactStat(2, MapFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("map_find_node_big", new OptScaleExactStat(5, MapFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("prophecy_coin_find_node", new OptScaleExactStat(2, ProphecyCoinFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("prophecy_coin_find_node_big", new OptScaleExactStat(5, ProphecyCoinFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("watcher_eye_find_node", new OptScaleExactStat(3, WatcherEyeFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("watcher_eye_find_node_big", new OptScaleExactStat(8, WatcherEyeFind.getInstance(), ModType.FLAT)));

        // event-chance nodes: raise the chance the league encounter spawns as bonus content in a map
        entry(PerkBuilder.stat("prophecy_event_chance_node", new OptScaleExactStat(2, ProphecyEventChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("prophecy_event_chance_node_big", new OptScaleExactStat(5, ProphecyEventChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("harvest_event_chance_node", new OptScaleExactStat(2, HarvestEventChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("harvest_event_chance_node_big", new OptScaleExactStat(5, HarvestEventChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("obelisk_event_chance_node", new OptScaleExactStat(2, ObeliskEventChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("obelisk_event_chance_node_big", new OptScaleExactStat(5, ObeliskEventChance.getInstance(), ModType.FLAT)));
    }

    private static void entry(Perk perk) {
        perk.is_entry = true;
    }
}
