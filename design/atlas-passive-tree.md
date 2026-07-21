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
| Singular Focus (one per league) | multi-stat perks, `one_kind = "singular_focus"` | ✅ | 3 mutually-exclusive perks; each grants +50 to its league's event-chance stat and −50 to the other two, reusing the `GET_MAP_CONTENT_WEIGHT_BONUS` weight path. Required un-flooring that listener (was `max(0, …)`) so the −50 penalty applies; existing event-chance stats are ≥0 so unaffected |

## Cluster 7 — New encounters  (MapContent marker block + trigger; no NBT editing)
Side content spawns by scattering a marker `block_id` per-chunk during map gen
(`MapBonusContentsData.calcSpawnChance`), exactly like Harvest/Obelisk. Each is a MapContent entry
+ custom block + trigger logic, with its own chance/density/potency wheel.
- 🔵 **Strongbox** — guarded locked chest (simplest; static guarded chest)
- 🔵 **Rift / Breach** — timed mob-spew portal → cache
- 🔵 **Imprisoned Monster / Essence** — caged mob, guaranteed typed reward on kill
- 🔵 **Shrine** — buff totem
- Backburner: Legion; Ritual (overlaps Prophecy)

## Parked
- Cluster 8 (mapping efficiency) — dropped.
- Cluster 9 (Atlas keystones) — concept kept, specific keystones TBD.

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
