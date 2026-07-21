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

        // per-rarity monster chance nodes: bias the mob rarity roll toward one specific rarity (OnMobSpawn).
        // Higher rarities use larger multipliers since their base spawn weight is much lower. First-pass values.
        entry(PerkBuilder.stat("uncommon_monster_chance_node", new OptScaleExactStat(3, UncommonMonsterChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("uncommon_monster_chance_node_big", new OptScaleExactStat(8, UncommonMonsterChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("rare_monster_chance_node", new OptScaleExactStat(4, RareMonsterChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("rare_monster_chance_node_big", new OptScaleExactStat(10, RareMonsterChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("epic_monster_chance_node", new OptScaleExactStat(5, EpicMonsterChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("epic_monster_chance_node_big", new OptScaleExactStat(12, EpicMonsterChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("legendary_monster_chance_node", new OptScaleExactStat(6, LegendaryMonsterChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("legendary_monster_chance_node_big", new OptScaleExactStat(15, LegendaryMonsterChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("mythic_monster_chance_node", new OptScaleExactStat(8, MythicMonsterChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("mythic_monster_chance_node_big", new OptScaleExactStat(20, MythicMonsterChance.getInstance(), ModType.FLAT)));

        // additional map boss chance: player-stat parallel to the EXTRA_MAP_BOSS_CHANCE relic stat (MapBossMB)
        entry(PerkBuilder.stat("additional_boss_chance_node", new OptScaleExactStat(10, AdditionalBossChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("additional_boss_chance_node_big", new OptScaleExactStat(25, AdditionalBossChance.getInstance(), ModType.FLAT)));
        // boss loot quantity: killer-side loot multiplier applied when a Boss-rarity mob is slain (LootInfo)
        entry(PerkBuilder.stat("boss_loot_quantity_node", new OptScaleExactStat(6, BossLootQuantity.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("boss_loot_quantity_node_big", new OptScaleExactStat(15, BossLootQuantity.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("currency_find_node", new OptScaleExactStat(2, CurrencyFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("currency_find_node_big", new OptScaleExactStat(5, CurrencyFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("omen_find_node", new OptScaleExactStat(2, OmenFind.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("omen_find_node_big", new OptScaleExactStat(5, OmenFind.getInstance(), ModType.FLAT)));

        // loot quantity/quality nodes (stats already exist + are consumed in LootInfo/rarity rolls)
        entry(PerkBuilder.stat("item_find_node", new OptScaleExactStat(1, TreasureQuantity.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("item_find_node_big", new OptScaleExactStat(3, TreasureQuantity.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("magic_find_node", new OptScaleExactStat(1, TreasureQuality.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("magic_find_node_big", new OptScaleExactStat(3, TreasureQuality.getInstance(), ModType.FLAT)));
        // Mythic extra drops (killer-side loot multiplier gated on Mythic rarity, LootInfo)
        entry(PerkBuilder.stat("extra_drop_from_mythics_node", new OptScaleExactStat(6, ExtraDropFromMythics.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("extra_drop_from_mythics_node_big", new OptScaleExactStat(15, ExtraDropFromMythics.getInstance(), ModType.FLAT)));
        // stat roll quality: biases dropped gear affix rolls toward their max (GearCreationUtils)
        entry(PerkBuilder.stat("stat_roll_quality_node", new OptScaleExactStat(3, StatRollQuality.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("stat_roll_quality_node_big", new OptScaleExactStat(8, StatRollQuality.getInstance(), ModType.FLAT)));

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
        // map rarity bias: raises the chance dropped maps upgrade to a higher rarity (MapBlueprint upgrade roll)
        entry(PerkBuilder.stat("map_rarity_bias_node", new OptScaleExactStat(4, MapRarityBias.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("map_rarity_bias_node_big", new OptScaleExactStat(10, MapRarityBias.getInstance(), ModType.FLAT)));
        // duplicate map: chance a completed map's boss also drops an exact copy of the run map
        entry(PerkBuilder.stat("duplicate_map_chance_node", new OptScaleExactStat(3, DuplicateMapChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("duplicate_map_chance_node_big", new OptScaleExactStat(8, DuplicateMapChance.getInstance(), ModType.FLAT)));
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

        // additional event chance: player-stat parallel to the BONUS_CONTENT_CHANCE relic stat -
        // rolls for one more bonus event (league encounter) when a map starts.
        entry(PerkBuilder.stat("double_event_chance_node", new OptScaleExactStat(3, DoubleEventChance.getInstance(), ModType.FLAT)));
        entry(PerkBuilder.stat("double_event_chance_node_big", new OptScaleExactStat(8, DoubleEventChance.getInstance(), ModType.FLAT)));

        // Singular Focus: mutually exclusive (one_kind) league-focus nodes. Each grants +50% event
        // weight to its own league and -50% to the others, via the existing GET_MAP_CONTENT_WEIGHT_BONUS
        // path (which reads these same event-chance stats per content). Only one may be allocated.
        oneOfAKind(PerkBuilder.stat("singular_focus_prophecy",
                new OptScaleExactStat(50, ProphecyEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(-50, HarvestEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(-50, ObeliskEventChance.getInstance(), ModType.FLAT)), "singular_focus");
        oneOfAKind(PerkBuilder.stat("singular_focus_harvest",
                new OptScaleExactStat(50, HarvestEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(-50, ProphecyEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(-50, ObeliskEventChance.getInstance(), ModType.FLAT)), "singular_focus");
        oneOfAKind(PerkBuilder.stat("singular_focus_obelisk",
                new OptScaleExactStat(50, ObeliskEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(-50, ProphecyEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(-50, HarvestEventChance.getInstance(), ModType.FLAT)), "singular_focus");
    }

    private static void entry(Perk perk) {
        perk.is_entry = true;
    }

    private static void oneOfAKind(Perk perk, String kind) {
        perk.is_entry = true;
        perk.one_kind = kind;
    }
}
