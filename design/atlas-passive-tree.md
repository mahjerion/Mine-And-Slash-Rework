# Atlas Passive Tree — perk design spec

Working design for expanding the Atlas passive tree (`data/mmorpg/mmorpg_talent_tree/atlas_passives.json`,
built from `aoe_data/database/perks/AtlasPassivePerks.java`) toward ~130 allocatable points.

Core principle (per project convention): **"more league X" stats raise the chance the encounter
*spawns* (MapContent weight / event count), never the reward.** Find-stats are only for genuinely
probabilistic drops. Avoid per-tick / per-event tracking; read stats at spawn / loot-roll time.

Rarity ladder in this mod (relevant because we go beyond PoE's Rare):
`Common → Uncommon → Rare → Epic → Legendary → Mythic` (+ special: Uber, Pinnacle, Boss).

## Mechanical patterns already in the codebase
- **Find stats** — a `Stat` read in a `BaseLootGen.baseDropChance()`, multiplying the drop chance
  (`OmenFind` in `OmenLootGen`). Adding one = new `Stat` + one line in the matching loot gen.
- **Density stats** — read in `OnMobSpawn` (`MobModifierDensity` biases the rarity roll) and in the
  pack-spawn data block `MobHordeMB` (`PackSize` via `MobPackSizeEffect`).
- **Event-chance stats** — raise a `MapContent` weight so a league encounter is picked among a map's
  bonus contents (`MapBonusContentsData.setupOnMapStart`, via `GetMapContentWeightBonusEvent`). The
  number of bonus contents per map is already `> 1`-capable (`bonus` count, +1 from the
  `BONUS_CONTENT_CHANCE` relic stat).

## Status legend
- ✅ **done** — stat exists, consumed, on tree
- 🟢 **cheap** — new `Stat` + one-line read in an existing hook (loot gen / existing consumer)
- 🟡 **hook** — needs a new/modified game-logic hook (spawn logic, map completion, gear gen)
- 🔵 **new content** — new MapContent encounter (marker block + trigger)

---

## Cluster 1 — Monster density & rarity  (hook: `OnMobSpawn`, `MobHordeMB`)
| Perk | Stat | Status | Notes |
|---|---|---|---|
| Pack Size | `PackSize` | ✅ | mobs per pack; already on tree + consumed |
| Mob Modifier Density | `MobModifierDensity` | ✅ | biases rarity roll; on tree |
| Rarity chances | `UncommonMonsterChance` / `RareMonsterChance` / `EpicMonsterChance` / `LegendaryMonsterChance` / `MythicMonsterChance` | ✅ | per-rarity weight bonus threaded through `Unit.WeightedMobRarity` (stacks with global density); gathered in `OnMobSpawn`. First-pass node values, tunable |
| High-rarity extra modifier | `HighRarityExtraModifier` | 🟡 | Rare+ mobs roll an extra affix |
| ~~Pack count~~ | ~~`PackCount`~~ | ❌ dropped | Redundant with pack size: packs spawn as a blob at a single point, so "more packs" and "bigger pack" collapse to the same effect. Would only be distinct if extra packs were spatially scattered (via `SpawnPointHelper`); decided not worth it |
| Pack leader | `PackLeaderChance` | 🟡 | normal packs get a magic leader |
| **Keystone** Purge the Weak | — | 🟡 | no magic monsters; greatly increased Rare-and-above chance |

## Cluster 2 — Map bosses  (maps always have a boss; spawn/difficulty only)
| Perk | Stat | Status | Notes |
|---|---|---|---|
| Additional boss | `AdditionalBossChance` | ✅ | player-stat parallel to the `EXTRA_MAP_BOSS_CHANCE` relic stat; rolled in `MapBossMB` via `GetExtraMapBossChanceEvent`. Currently +1 boss max (extendable to multi) |
| ~~Boss extra affixes~~ | ~~`BossExtraAffixChance`~~ | ❌ dropped | would need the mob-affix system fleshed out first (`MobData.randomizeAffixes` is a stub adding max 1 affix); not worth that broader change now |
| Boss extra drops | `BossLootQuantity` | ✅ | killer-side loot multiplier in `LootInfo`, gated on `IRarity.BOSS`. NOTE: player-side, so in multiplayer only the killer's stat applies (unlike spawn-time stats which take the party max) — accepted tradeoff |

## Cluster 3 — Loot quality
| Perk | Stat | Status | Notes |
|---|---|---|---|
| Item Find | `TreasureQuantity` | 🟢 | stat exists + consumed (`LootInfo`); just needs tree entry |
| Magic Find | `TreasureQuality` | 🟢 | stat exists + consumed; just needs tree entry |
| Mythic extra drops | `ExtraDropFromMythics` | ✅ | killer-side loot multiplier in `LootInfo` gated on `IRarity.MYTHIC_ID` (same shape as `BossLootQuantity`; killer-only in multiplayer) |
| Stat roll quality | `StatRollQuality` | ✅ | biases each dropped-gear affix's roll percentile toward its max (lerp `p`→max by quality%) in `GearCreationUtils.CreateData`. Initial drop roll only — crafting/reroll paths untouched. No extra sockets |

## Cluster 4 — Map sustain
| Perk | Stat | Status | Notes |
|---|---|---|---|
| Map Find | `MapFind` | ✅ | on tree + consumed (`MapLootGen`) |
| Map Rarity | `MapRarityBias` | ✅ | feeds `GearRarityPart`'s higher-rarity upgrade roll in `MapBlueprint.createData`. Required threading the player into `OnGenerateNewMapItemEvent` (the drop path previously used a dummy level-1 context, so map rarity never upgraded at all). DESIGN CHOICE: only the dedicated stat applies, not general magic find — map rarity stays gated behind the Atlas |
| Duplicate Map | `DuplicateMapChance` | ✅ | killer-side roll at final-boss death (`DungeonEvents`) drops an exact copy of the run map rebuilt from `getSnapshotStack()`. Via `GetDuplicateMapChanceEvent`; multiplayer = killer only (like the uber-frag drop sharing this hook) |
| Relic Find | `RelicFind` | ✅ | per-relic bonus roll: for each of the boss-kill guaranteed relics (3 Uber / 1 final boss), a killer-side roll via `GetRelicFindBonusEvent` grants one extra (`DungeonEvents`, same hook as Duplicate Map/Uber Fragment). Also applied in `ON_CHEST_LOOTED` to give dungeon chests a chance to contain a bonus relic — the first item ever granted from that hook, which previously only tracked `lootedChests` for completion. At 0 the chest path is a no-op, so default behavior is unchanged without Atlas investment |

## Cluster 5 — Reward-type find wheels
| Perk | Stat | Status | Notes |
|---|---|---|---|
| Omen / Prophecy Coin / Watcher Eye / Uber Fragment | existing find stats | ✅ | on tree |
| Skill Gem Find | `SkillGemFind` | ✅ | skill gems; read in `AuraGemLootGen` + `SuppGemLootGen`; global, not map-bound |
| Gem Find | `GemFind` | ✅ | socketable gems; read in `GemLootGen`; distinct from skill gems |
| Rune Find | `RuneFind` | ✅ | read in `RuneLootGen` |
| Jewel Find | `JewelFind` | ✅ | read in `JewelLootGen` |

## Cluster 6 — League event chance
| Perk | Stat | Status | Notes |
|---|---|---|---|
| Prophecy / Harvest / Obelisk event chance | existing | ✅ | raise MapContent weight |
| Double event | `DoubleEventChance` | ✅ | player-stat parallel to `BONUS_CONTENT_CHANCE`; rolls for +1 bonus content in `MapBonusContentsData` via `GetBonusContentChanceEvent` |
| Strongbox event chance | `StrongboxEventChance` | ✅ | opener-only weight boost for the strongbox encounter, mapped in `GET_MAP_CONTENT_WEIGHT_BONUS` |
| Singular Focus (one per event) | perks, `one_kind = "singular_focus"` + shared `EventFocusPenalty` | ✅ | 4 mutually-exclusive perks (prophecy/harvest/obelisk/strongbox). Each grants +50 to its own event-chance stat AND +50 to the single shared `EventFocusPenalty`, which the weight listener subtracts from EVERY event. Net: focused event unpenalized, all others −50%. Scalable — new events are covered with no edits to existing focus nodes. (Listener also un-floored from `max(0,…)` so negatives apply.) |

## Cluster 6b — In-league reward quantity (distinct from Cluster 6's spawn-chance stats)
| Perk | Stat | Status | Notes |
|---|---|---|---|
| Obelisk Extra Drops | `ObeliskExtraDrops` | ✅ | player-stat parallel to the `TRIPLE_CHEST_REWARD_CHANCE` relic stat; scales the reward-chest count computed in `ObeliskRewardLogic.spawnChests` via a new `GetObeliskChestBonusEvent` cross-module query (`ObeliskExileEvents`, consumed in the new `ObeliskAddonEvents` glue class) |
| Harvest Extra Drops | `HarvestExtraDrops` | ✅ | scales the per-mob-kill Harvest loot-table trigger chance (`HarvestConfig.LOOT_TABLE_CHANCE_PER_MOB`) in `HarvestMain`'s `LivingDeathEvent` listener via a new `GetHarvestLootBonusEvent` query (`HarvestExileEvents`, consumed in the new `HarvestAddonEvents` glue class) - the first in-encounter reward stat for Harvest, no prior relic stat to mirror |
| Strongbox Extra Drops | `StrongboxExtraDrops` | ✅ | see Cluster 7 |
| Imprisoned Monster Extra Drops | `ImprisonedMonsterExtraDrops` | ✅ | see Cluster 7 |

## Cluster 7 — New encounters  (MapContent marker block + trigger; no NBT editing)
Side content spawns by scattering a marker `block_id` per-chunk during map gen
(`MapBonusContentsData.calcSpawnChance`), exactly like Harvest/Obelisk. Each is a MapContent entry
+ custom block + trigger logic, with its own chance/density/potency wheel.
- ✅ **Strongbox** — `StrongboxBlock` (BaseEntityBlock) + `StrongboxBE` + `MnsMapContents.STRONGBOX` (weight 1000, 1-2 per map), in the dungeon-realm glue package so it can use the dungeon mob pool. **Locked**: right-click releases a guardian pack (8) drawn from the current dungeon's `DUNGEON_MOB_SPAWNS` (auto-scaled, persistent, targeting the opener); the BE ticker checks each second and only when every guardian is dead does it **unlock** — spilling a richer reward (independent `ofChestLoot` rolls, map-chest boosted, plus a guaranteed weighted-category item roll — see below) to the nearest present player, then removing the block. Guardians **count toward map completion**: flagged `isDungeonMob` (deaths → `mobKills`) and added to `mobSpawnCount`, so opening a box adds to the kills needed for 100%. Atlas: `StrongboxEventChance` + `singular_focus_strongbox` (DoubleEventChance already generic) for spawn odds, plus `StrongboxExtraDrops` (reward-quantity, see below) for the guaranteed category-item count. Placeholder gold/stone model (no new texture yet)
- 🔵 **Rift / Breach** — timed mob-spew portal → cache
- ✅ **Imprisoned Monster / Essence** — `ImprisonedMonsterBlock` (BaseEntityBlock) + `ImprisonedMonsterBE` + `MnsMapContents.IMPRISONED_MONSTER` (weight 1000, 1 per map), glue package. Right-click releases a single caged mob forced to **Boss** rarity (`ExileDB.MobRarities().get(IRarity.BOSS)` + `DungeonAddonUtil.createMobRarityEdit` — the same mechanism the real per-map dungeon boss uses), from the dungeon pool; affixes are explicitly re-rolled for the forced rarity right after (`MobData.randomizeAffixes`), since the automatic first spawn pass already rolled — and the rarity-override pass skips re-rolling — affixes for whatever rarity got picked before the override. BE ticker watches it; on its death the encounter pays a **guaranteed** reward — `DungeonConfig.IMPRISONED_MONSTER_CURRENCY_REWARD` (default 3) currency items via `CurrencyLootGen.generateOne()` (bypassing its chance roll), scaled by the Atlas `ImprisonedMonsterExtraDrops` multiplier — plus a chance (`dropPotentialSeed`; `DungeonConfig.IMPRISONED_MONSTER_SEED_DROP_CHANCE`, default 25%, of which `IMPRISONED_MONSTER_PERFECTED_SEED_CHANCE`, default 10%, is the Perfected tier, only rollable once the encounter's loot level ≥ `MIN_LEVEL_FOR_PERFECTED_SEED` (default 80) - below that only base Seeds can drop) at one of the "Seed" currencies below, to the nearest present player, then removes the block. All four numbers are `DungeonConfig` server-config values (`RandomUtils.roll(...)`, the same percent-roll helper `UBER_FRAG_DROPRATE` uses). Counts toward completion as a **mini-boss** (`isMiniBossMob`, `miniBossSpawnCount++` → mini-boss completion weight, up from elite weight, reflecting the harder fight). Atlas: `ImprisonedMonsterEventChance` + `singular_focus_imprisoned_monster` (mapped "imprisoned_monster" in the weight listener; DoubleEventChance already generic) for spawn odds, plus `ImprisonedMonsterExtraDrops` for the guaranteed currency count. Placeholder crying-obsidian/iron-bars model. TODO: swap the guaranteed currency items for the planned dedicated reward item type
  - **"Seed" currencies** (`orbs_of_crafting/currency/reworked/ImprisonedMonsterCurrencies.java`, registered on `ExileCurrencies.INSTANCE.IMPRISONED_MONSTER`): 7 base (Might/Mind/Heart/Root/Stride/Soul/Bond Seed — Weapon/Helmet/Chest/Legs/Boots/Necklace/Ring respectively) + 7 "Perfected" variants (e.g. "Perfected Mind Seed"), each a one-time-use `ExileCurrency` that restores Potential to a gear item (base: +20 via `ItemMods.ADD_20_POTENTIAL`, Perfected: +50 via `ADD_50_POTENTIAL` — both just parametrized instances of the existing `AddPotentialItemMod`). Slot-gated via a new `IsGearSlotTagReq` (checks `BaseGearType.tags` against `SlotTags.weapon_family/helmet/chest/pants/boots/necklace/ring`, registered as `ItemReqs.IS_GEAR_SLOT`, one `ExileKeyMap` entry per slot). Once-per-item, across ALL 14, enforced by reusing the existing `MaximumUsesReq`/`MaxUsesKey` one-time-use machinery: every Seed shares the same `ItemReqs.Datas.SEED_USES` data (`"seed_uses"`, max 1) via `.edit(MaxUsesKey.ofUses(...))`, so applying any one of them (base or Perfected) increments the same NBT counter and blocks every other one on that item — no bespoke flag/requirement needed, same pattern `MAX_OMEN_RARITY_USES` already uses. Registered with `weight(0)`: exclusive to this encounter, never in the general weighted currency loot pool. Placeholder texture (reused `entangled_potential.png`, the existing "add Potential" currency) on all 14 items pending real art
- ✅ **Shrine** — `ShrineBlock` (plain one-shot `Block`, no BE) + `MnsMapContents.SHRINE` (weight 1000, 1-2 per map), glue package. Right-click **once** picks a **weighted-random buff from the datapack-driven `ShrineBuff` registry** and grants it to EVERY player within radius (12) — the user and any allies nearby — via the exile_effect system (`ExilePotionEvent` + `GiveOrTake2.give`, self-cast on each player), then the shrine is consumed immediately. Each recipient gets a chat message (`Chats.SHRINE_BUFF_RECEIVED`) naming the buff via the effect's own localized name (`ExileEffect#locName()` nested as a translation arg). On use, also bursts a single ring of `ENCHANT` particles at exactly the buff radius (`ServerLevel#sendParticles`, ring point count scaled to circumference) so the radius reads visually. No mobs / no ticker, so it does **not** touch map completion. Atlas: `ShrineEventChance` + `singular_focus_shrine` (mapped "shrine" in the weight listener; DoubleEventChance already generic) for spawn odds, plus a reward-quality node (see below) instead of a dedicated extra-drops stat, since Shrine payout is a buff, not an item count. Placeholder lodestone/gold model
  - **`ShrineBuff` datapack content type** (`database/data/shrine/`, registry type `SHRINE_BUFF` order 48, `ExileDB.ShrineBuffs()`): each entry = one buff option (`effect_id` = ExileEffect GUID, `weight`, `duration_ticks`). The registry *is* the list the shrine rolls from — `ExileDB.ShrineBuffs().random()` (weighted). Datapackers add options by dropping another JSON; no code change. Defaults built in `ShrineBuffs.init()` (called from `GeneratedData`) — currently just `valor` (1000 wt, 60s). Was previously a hardcoded `ModEffects.VALOR` constant on the block. Any effect used here should carry `EffectTags.shrine` (see below).
  - **Shrine buff effectiveness on you** — rather than a dedicated `Stat`, reuses the existing generic per-`EffectTag` stat family `EffectStats.EFFECT_OF_BUFFS_ON_YOU_PER_EFFECT_TAG` (`inc_effect_of_<tag>_buff_on_you`). A new `EffectTags.shrine` tag was added and applied to `ModEffects.VALOR` (which also strengthens Valor when granted by non-Shrine sources, e.g. the Wandering Bard perk — accepted, matching how the sibling per-tag stats already work generically). Atlas nodes reference the generated instance directly: `EffectStats.EFFECT_OF_BUFFS_ON_YOU_PER_EFFECT_TAG.get(EffectTags.shrine)`.
- Backburner: Legion; Ritual (overlaps Prophecy)

## Cluster 9 — Atlas keystones
One drawback/upside "gamechanger" perk per Cluster-7 bonus encounter, plus one for Harvest. First use
of `PerkBuilder.gameChanger(id, locname, stats...)` (`Perk.PerkType.MAJOR`). Icons are placeholders
pending art (`textures/gui/stat_icons/game_changers/<id>.png`).

**Not yet placed on the tree grid.** `data/mmorpg/mmorpg_talent_tree/atlas_passives.json`'s `perks` CSV
grid is hand/tool-authored (`TalentTree.shouldGenerateJson()` returns `false` - "easier to use a
program for a grid than to do it in code"), unlike the rest of `aoe_data`. `./gradlew runData`
registers these 6 perks into the perk database (so their stats are valid and `PerkBuilder.gameChanger`
resolved correctly) but does **not** place them into the grid - that's a separate manual/tool step.
Until placed, they exist but aren't reachable by players in the Atlas Passive Tree screen.

| Perk | Stats | Status | Notes |
|---|---|---|---|
| Unique Windfall | `StrongboxUniqueChance` + `StrongboxGuardianToughness` | ✅ | +100% chance the strongbox's guaranteed category roll is Unique; guardians get +50% HP/damage (not higher rarity) via a second `AttributeModifier` pair in `StrongboxBlock.spawnGuardians()`/`unlock()` |
| Twin Curse | `ProphecyDoubleCurse` (marker) + reused `ProphecyCoinFind` | ✅ | Forces 2 curse picks per altar (`ProphecyAltarBlock.use()` bumps `numMobAffixesCanAdd` by 2 — the existing "must spend before reuse" gate does the rest) for +30% Prophecy Coin find |
| Twin Blessing | `ShrineDoubleBuff` (marker) | ✅ | Per-player: with the perk, `ShrineBlock.grantBuff()` rolls a second independent buff and shortens both durations ×0.75; `Chats.SHRINE_BUFF_RECEIVED_TWO` covers the 2-buff message |
| Twin Captives | `ImprisonedMonsterDoubleSpawn` (marker) | ✅ | Releases 2 caged Boss-rarity mobs (`ImprisonedMonsterBE.monsters` is now a list); reward only pays out once both are dead, scaled 2x total (not per-mob) |
| Greater Trial | `ObeliskMobToughness` + reused `ObeliskExtraDrops` | ✅ | New `GetObeliskMobToughnessEvent`/`ObeliskExileEvents.GET_MOB_TOUGHNESS_BONUS` query (party max, mirrors the chest-bonus event) scales a second HP/damage modifier pair in `ObeliskMobTierStats.tryApply()`, queried once at mob spawn; chest count reuses the existing Obelisk Extra Drops path |
| Bountiful Aftermath | `HarvestCompletionBounty` (marker) | ✅ | New `HarvestCompletedEvent`/`HarvestExileEvents.HARVEST_COMPLETED` notification fires once when a Harvest's timer runs out; main mod grants the new `ModEffects.HARVEST_BOUNTY` exile_effect (+40% Currency Find, 2 min) to players with the perk |

## Parked
- Cluster 8 (mapping efficiency) — dropped.

---

## Implementation order
1. **Cheap batch (this pass)** — `TreasureQuantity`/`TreasureQuality` tree entries; `GemFind` +
   `SkillGemFind` stats, consumption, registration, tree entries. (`RuneFind`/`JewelFind` are the
   same shape — fast follow.)
2. Density-split + boss hooks (`OnMobSpawn`, `MobHordeMB`).
3. Map sustain / quality hooks.
4. Cluster 6 event-count + Singular Focus.
5. Cluster 7 encounters (Strongbox first).

After any `aoe_data` change, run `./gradlew runData` to regenerate `src/generated/resources`.
