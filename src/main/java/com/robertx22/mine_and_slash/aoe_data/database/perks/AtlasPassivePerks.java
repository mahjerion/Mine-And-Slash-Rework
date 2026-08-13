package com.robertx22.mine_and_slash.aoe_data.database.perks;

import com.robertx22.mine_and_slash.aoe_data.database.stats.EffectStats;
import com.robertx22.mine_and_slash.database.OptScaleExactStat;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.*;
import com.robertx22.mine_and_slash.tags.all.EffectTags;
import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;

// Flat, player-wide stat perks placed on the Atlas passive tree (data/mmorpg/mmorpg_talent_tree/atlas_passives.json).
// Only the 3 "starter" perks are entry perks (allocatable with no prerequisite); every other node is a
// non-entry perk that must be reached through the tree. PerkBuilder.stat(...) already registers the perk,
// so a plain call = a non-entry node; entry(...) additionally flags it as an entry.
public class AtlasPassivePerks {

    public static void init() {

        //entry perks (the only 3 allocatable with no prerequisite)
        entry(PerkBuilder.stat("map_find_entry", new OptScaleExactStat(1, MapFind.getInstance(), ModType.FLAT)));

        //dedicated connector perks for traversing between clusters on the tree
        PerkBuilder.stat("map_find", new OptScaleExactStat(1, MapFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("map_rarity_bias", new OptScaleExactStat(1, MapRarityBias.getInstance(), ModType.FLAT));
        PerkBuilder.stat("mob_modifier_density", new OptScaleExactStat(1, MobModifierDensity.getInstance(), ModType.FLAT));

        //generic mob modifiers
        PerkBuilder.stat("pack_size_node", new OptScaleExactStat(3, PackSize.getInstance(), ModType.FLAT));
        PerkBuilder.stat("pack_size_node_big", new OptScaleExactStat(8, PackSize.getInstance(), ModType.FLAT));
        PerkBuilder.stat("mob_modifier_density_node", new OptScaleExactStat(2, MobModifierDensity.getInstance(), ModType.FLAT));
        PerkBuilder.stat("mob_modifier_density_node_big", new OptScaleExactStat(5, MobModifierDensity.getInstance(), ModType.FLAT));

        // per-rarity monster chance nodes: bias the mob rarity roll toward one specific rarity (OnMobSpawn).
        // Higher rarities use larger multipliers since their base spawn weight is much lower. First-pass values.
        PerkBuilder.stat("uncommon_monster_chance_node", new OptScaleExactStat(3, UncommonMonsterChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("uncommon_monster_chance_node_big", new OptScaleExactStat(8, UncommonMonsterChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("rare_monster_chance_node", new OptScaleExactStat(4, RareMonsterChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("rare_monster_chance_node_big", new OptScaleExactStat(10, RareMonsterChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("epic_monster_chance_node", new OptScaleExactStat(5, EpicMonsterChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("epic_monster_chance_node_big", new OptScaleExactStat(12, EpicMonsterChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("legendary_monster_chance_node", new OptScaleExactStat(6, LegendaryMonsterChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("legendary_monster_chance_node_big", new OptScaleExactStat(15, LegendaryMonsterChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("mythic_monster_chance_node", new OptScaleExactStat(8, MythicMonsterChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("mythic_monster_chance_node_big", new OptScaleExactStat(20, MythicMonsterChance.getInstance(), ModType.FLAT));

        // additional map boss chance: player-stat parallel to the EXTRA_MAP_BOSS_CHANCE relic stat (MapBossMB)
        PerkBuilder.stat("additional_boss_chance_node", new OptScaleExactStat(5, AdditionalBossChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("additional_boss_chance_node_big", new OptScaleExactStat(12, AdditionalBossChance.getInstance(), ModType.FLAT));
        // boss loot quantity: killer-side loot multiplier applied when a Boss-rarity mob is slain (LootInfo)
        PerkBuilder.stat("boss_loot_quantity_node", new OptScaleExactStat(4, BossLootQuantity.getInstance(), ModType.FLAT));
        PerkBuilder.stat("boss_loot_quantity_node_big", new OptScaleExactStat(10, BossLootQuantity.getInstance(), ModType.FLAT));
        PerkBuilder.stat("currency_find_node", new OptScaleExactStat(2, CurrencyFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("currency_find_node_big", new OptScaleExactStat(5, CurrencyFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("omen_find_node", new OptScaleExactStat(2, OmenFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("omen_find_node_big", new OptScaleExactStat(5, OmenFind.getInstance(), ModType.FLAT));

        // loot quantity/quality nodes (stats already exist + are consumed in LootInfo/rarity rolls)
        PerkBuilder.stat("item_find_node", new OptScaleExactStat(1, TreasureQuantity.getInstance(), ModType.FLAT));
        PerkBuilder.stat("item_find_node_big", new OptScaleExactStat(3, TreasureQuantity.getInstance(), ModType.FLAT));
        PerkBuilder.stat("magic_find_node", new OptScaleExactStat(1, TreasureQuality.getInstance(), ModType.FLAT));
        PerkBuilder.stat("magic_find_node_big", new OptScaleExactStat(3, TreasureQuality.getInstance(), ModType.FLAT));
        // Mythic extra drops (killer-side loot multiplier gated on Mythic rarity, LootInfo)
        PerkBuilder.stat("extra_drop_from_mythics_node", new OptScaleExactStat(4, ExtraDropFromMythics.getInstance(), ModType.FLAT));
        PerkBuilder.stat("extra_drop_from_mythics_node_big", new OptScaleExactStat(10, ExtraDropFromMythics.getInstance(), ModType.FLAT));
        // stat roll quality: biases dropped gear affix rolls toward their max (GearCreationUtils)
        PerkBuilder.stat("stat_roll_quality_node", new OptScaleExactStat(2, StatRollQuality.getInstance(), ModType.FLAT));
        PerkBuilder.stat("stat_roll_quality_node_big", new OptScaleExactStat(5, StatRollQuality.getInstance(), ModType.FLAT));

        // gem find nodes: socketable gems (GemLootGen) and skill gems (Aura/Supp gem gens)
        PerkBuilder.stat("gem_find_node", new OptScaleExactStat(2, GemFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("gem_find_node_big", new OptScaleExactStat(5, GemFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("skill_gem_find_node", new OptScaleExactStat(2, SkillGemFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("skill_gem_find_node_big", new OptScaleExactStat(5, SkillGemFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("rune_find_node", new OptScaleExactStat(2, RuneFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("rune_find_node_big", new OptScaleExactStat(5, RuneFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("jewel_find_node", new OptScaleExactStat(2, JewelFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("jewel_find_node_big", new OptScaleExactStat(5, JewelFind.getInstance(), ModType.FLAT));

        // find/quantity nodes for probabilistic drops
        PerkBuilder.stat("uber_fragment_find_node", new OptScaleExactStat(3, UberFragmentFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("uber_fragment_find_node_big", new OptScaleExactStat(8, UberFragmentFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("map_find_node", new OptScaleExactStat(2, MapFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("map_find_node_big", new OptScaleExactStat(5, MapFind.getInstance(), ModType.FLAT));
        // map rarity bias: raises the chance dropped maps upgrade to a higher rarity (MapBlueprint upgrade roll)
        PerkBuilder.stat("map_rarity_bias_node", new OptScaleExactStat(3, MapRarityBias.getInstance(), ModType.FLAT));
        PerkBuilder.stat("map_rarity_bias_node_big", new OptScaleExactStat(8, MapRarityBias.getInstance(), ModType.FLAT));
        // duplicate map: chance a completed map's boss also drops an exact copy of the run map
        PerkBuilder.stat("duplicate_map_chance_node", new OptScaleExactStat(3, DuplicateMapChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("duplicate_map_chance_node_big", new OptScaleExactStat(8, DuplicateMapChance.getInstance(), ModType.FLAT));
        // relic find: chance for boss relic drops to grant a bonus relic, and for dungeon chests to contain one
        PerkBuilder.stat("relic_find_node", new OptScaleExactStat(3, RelicFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("relic_find_node_big", new OptScaleExactStat(8, RelicFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("prophecy_coin_find_node", new OptScaleExactStat(2, ProphecyCoinFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("prophecy_coin_find_node_big", new OptScaleExactStat(5, ProphecyCoinFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("watcher_eye_find_node", new OptScaleExactStat(3, WatcherEyeFind.getInstance(), ModType.FLAT));
        PerkBuilder.stat("watcher_eye_find_node_big", new OptScaleExactStat(8, WatcherEyeFind.getInstance(), ModType.FLAT));

        // event-chance nodes: raise the chance the league encounter spawns as bonus content in a map
        PerkBuilder.stat("prophecy_event_chance_node", new OptScaleExactStat(2, ProphecyEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("prophecy_event_chance_node_big", new OptScaleExactStat(5, ProphecyEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("harvest_event_chance_node", new OptScaleExactStat(2, HarvestEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("harvest_event_chance_node_big", new OptScaleExactStat(5, HarvestEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("obelisk_event_chance_node", new OptScaleExactStat(2, ObeliskEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("obelisk_event_chance_node_big", new OptScaleExactStat(5, ObeliskEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("strongbox_event_chance_node", new OptScaleExactStat(2, StrongboxEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("strongbox_event_chance_node_big", new OptScaleExactStat(5, StrongboxEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("imprisoned_monster_event_chance_node", new OptScaleExactStat(2, ImprisonedMonsterEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("imprisoned_monster_event_chance_node_big", new OptScaleExactStat(5, ImprisonedMonsterEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("shrine_event_chance_node", new OptScaleExactStat(2, ShrineEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("shrine_event_chance_node_big", new OptScaleExactStat(5, ShrineEventChance.getInstance(), ModType.FLAT));

        // reward-quantity nodes for the Cluster 7 bonus-map encounters (distinct from the event-chance
        // nodes above, which only affect whether the encounter spawns - not what it pays out)
        PerkBuilder.stat("strongbox_extra_drops_node", new OptScaleExactStat(4, StrongboxExtraDrops.getInstance(), ModType.FLAT));
        PerkBuilder.stat("strongbox_extra_drops_node_big", new OptScaleExactStat(10, StrongboxExtraDrops.getInstance(), ModType.FLAT));
        PerkBuilder.stat("imprisoned_monster_extra_drops_node", new OptScaleExactStat(4, ImprisonedMonsterExtraDrops.getInstance(), ModType.FLAT));
        PerkBuilder.stat("imprisoned_monster_extra_drops_node_big", new OptScaleExactStat(10, ImprisonedMonsterExtraDrops.getInstance(), ModType.FLAT));
        // reuses the generic per-EffectTag "effectiveness of X buffs on you" stat family
        // (EffectStats.EFFECT_OF_BUFFS_ON_YOU_PER_EFFECT_TAG) via the new `shrine` EffectTag
        PerkBuilder.stat("shrine_buff_effectiveness_node",
                new OptScaleExactStat(6, EffectStats.EFFECT_OF_BUFFS_ON_YOU_PER_EFFECT_TAG.get(EffectTags.shrine), ModType.FLAT));
        PerkBuilder.stat("shrine_buff_effectiveness_node_big",
                new OptScaleExactStat(15, EffectStats.EFFECT_OF_BUFFS_ON_YOU_PER_EFFECT_TAG.get(EffectTags.shrine), ModType.FLAT));
        // duration side of the same per-EffectTag idea (EFFECT_DURATION_YOU_CAST_PER_TAG -> shrine_eff_dur_u_cast).
        // that family is Source-side, but "you cast" is not a multiplayer footgun here: ShrineBlock loops the
        // players in range and fires one event per player with source == target == that player, so every
        // recipient is their own caster. each player's own node scales only their own buff - the player who
        // clicked the shrine does not extend anyone else's.
        // additive against the base duration, and Twin Blessing's 0.75 multi is applied before the event,
        // so the two compose multiplicatively.
        PerkBuilder.stat("shrine_buff_duration_node",
                new OptScaleExactStat(10, EffectStats.EFFECT_DURATION_YOU_CAST_PER_TAG.get(EffectTags.shrine), ModType.FLAT));
        PerkBuilder.stat("shrine_buff_duration_node_big",
                new OptScaleExactStat(25, EffectStats.EFFECT_DURATION_YOU_CAST_PER_TAG.get(EffectTags.shrine), ModType.FLAT));
        // Harvest/Obelisk in-encounter reward quantity (distinct from harvest_event_chance/obelisk_event_chance
        // above, which only affect encounter spawn odds) - player-stat parallel to Obelisk's
        // TRIPLE_CHEST_REWARD_CHANCE relic stat
        PerkBuilder.stat("harvest_extra_drops_node", new OptScaleExactStat(4, HarvestExtraDrops.getInstance(), ModType.FLAT));
        PerkBuilder.stat("harvest_extra_drops_node_big", new OptScaleExactStat(10, HarvestExtraDrops.getInstance(), ModType.FLAT));
        PerkBuilder.stat("obelisk_extra_drops_node", new OptScaleExactStat(4, ObeliskExtraDrops.getInstance(), ModType.FLAT));
        PerkBuilder.stat("obelisk_extra_drops_node_big", new OptScaleExactStat(10, ObeliskExtraDrops.getInstance(), ModType.FLAT));

        // additional event chance: player-stat parallel to the BONUS_CONTENT_CHANCE relic stat -
        // rolls for one more bonus event (league encounter) when a map starts.
        PerkBuilder.stat("double_event_chance_node", new OptScaleExactStat(2, DoubleEventChance.getInstance(), ModType.FLAT));
        PerkBuilder.stat("double_event_chance_node_big", new OptScaleExactStat(5, DoubleEventChance.getInstance(), ModType.FLAT));

        // Singular Focus: mutually exclusive (one_kind) map-event focus nodes. Each grants +100% to its
        // own event's chance AND +50% EventFocusPenalty (which the weight listener subtracts from EVERY
        // event). Net: the focused event is +50%, every other event is -50%. Using the single shared
        // penalty stat means new events are covered automatically - no need to edit these nodes.
        oneOfAKind(PerkBuilder.stat("singular_focus_prophecy",
                new OptScaleExactStat(50, ProphecyEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(25, EventFocusPenalty.getInstance(), ModType.FLAT)), "singular_focus");
        oneOfAKind(PerkBuilder.stat("singular_focus_harvest",
                new OptScaleExactStat(50, HarvestEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(25, EventFocusPenalty.getInstance(), ModType.FLAT)), "singular_focus");
        oneOfAKind(PerkBuilder.stat("singular_focus_obelisk",
                new OptScaleExactStat(50, ObeliskEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(25, EventFocusPenalty.getInstance(), ModType.FLAT)), "singular_focus");
        oneOfAKind(PerkBuilder.stat("singular_focus_strongbox",
                new OptScaleExactStat(50, StrongboxEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(25, EventFocusPenalty.getInstance(), ModType.FLAT)), "singular_focus");
        oneOfAKind(PerkBuilder.stat("singular_focus_imprisoned_monster",
                new OptScaleExactStat(50, ImprisonedMonsterEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(25, EventFocusPenalty.getInstance(), ModType.FLAT)), "singular_focus");
        oneOfAKind(PerkBuilder.stat("singular_focus_shrine",
                new OptScaleExactStat(50, ShrineEventChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(25, EventFocusPenalty.getInstance(), ModType.FLAT)), "singular_focus");

        // Cluster 9 - Atlas keystones: one drawback/upside "gamechanger" perk per Cluster-7 bonus
        // encounter, plus one for Harvest. First use of PerkBuilder.gameChanger.
        PerkBuilder.gameChanger("keystone_strongbox_unique_windfall", "Windfall",
                new OptScaleExactStat(100, StrongboxUniqueChance.getInstance(), ModType.FLAT),
                new OptScaleExactStat(30, StrongboxGuardianToughness.getInstance(), ModType.FLAT));
        PerkBuilder.gameChanger("keystone_prophecy_twin_curse", "Twin Curse",
                new OptScaleExactStat(1, ProphecyDoubleCurse.getInstance(), ModType.FLAT),
                new OptScaleExactStat(30, ProphecyCoinFind.getInstance(), ModType.FLAT));
        PerkBuilder.gameChanger("keystone_shrine_twin_blessing", "Villager's Blessing",
                new OptScaleExactStat(1, ShrineDoubleBuff.getInstance(), ModType.FLAT));
        PerkBuilder.gameChanger("keystone_imprisoned_monster_twin_captives", "Gem",
                new OptScaleExactStat(1, ImprisonedMonsterDoubleSpawn.getInstance(), ModType.FLAT));
        PerkBuilder.gameChanger("keystone_obelisk_greater_trial", "Doppelgangers",
                new OptScaleExactStat(25, ObeliskMobToughness.getInstance(), ModType.FLAT),
                new OptScaleExactStat(30, ObeliskExtraDrops.getInstance(), ModType.FLAT));
        PerkBuilder.gameChanger("keystone_harvest_bountiful_aftermath", "Bountiful Aftermath",
                new OptScaleExactStat(1, HarvestCompletionBounty.getInstance(), ModType.FLAT));
    }

    private static void entry(Perk perk) {
        perk.is_entry = true;
        perk.one_kind = "atlas_start";
    }

    // mutual-exclusivity marker only - does NOT make the perk an entry (must still be reached via the tree).
    private static void oneOfAKind(Perk perk, String kind) {
        perk.one_kind = kind;
    }
}
