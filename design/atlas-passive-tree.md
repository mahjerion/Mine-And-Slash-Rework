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
| Rarity chances | `UncommonMonsterChance` / `RareMonsterChance` / `EpicMonsterChance` / `LegendaryMonsterChance` / `MythicMonsterChance` | 🟡 | split the single density stat into per-rarity roll bonuses in `OnMobSpawn.randomRarity` |
| High-rarity extra modifier | `HighRarityExtraModifier` | 🟡 | Rare+ mobs roll an extra affix |
| Pack count | `PackCount` | 🟡 | after `MobHordeMB` spawns its pack, roll for one extra adjacent pack (no NBT edits) |
| Pack leader | `PackLeaderChance` | 🟡 | normal packs get a magic leader |
| **Keystone** Purge the Weak | — | 🟡 | no magic monsters; greatly increased Rare-and-above chance |

## Cluster 2 — Map bosses  (maps always have a boss; spawn/difficulty only)
| Perk | Stat | Status | Notes |
|---|---|---|---|
| Additional boss | `AdditionalBossChance` | 🟡 | chance a second boss spawns ("Twinned") |
| Boss extra affixes | `BossExtraAffixChance` | 🟡 | boss spawns with extra affixes |
| Boss extra drops | `ExtraMobDropsStat` (existing) | 🟡 | apply the existing `extra_mob_drops` stat to the boss |

## Cluster 3 — Loot quality
| Perk | Stat | Status | Notes |
|---|---|---|---|
| Item Find | `TreasureQuantity` | 🟢 | stat exists + consumed (`LootInfo`); just needs tree entry |
| Magic Find | `TreasureQuality` | 🟢 | stat exists + consumed; just needs tree entry |
| Mythic extra drops | `ExtraDropFromMythics` | 🟡 | Mythic monsters chance for an additional item |
| Stat roll quality | `StatRollQuality` | 🟡 | higher quality/roll-strength of stats on dropped gear (no extra sockets) |

## Cluster 4 — Map sustain
| Perk | Stat | Status | Notes |
|---|---|---|---|
| Map Find | `MapFind` | ✅ | on tree + consumed (`MapLootGen`) |
| Map Rarity | `MapRarityBias` | 🟡 | dropped maps skew higher rarity (tier follows rarity); modifies map drop roll |
| Duplicate Map | `DuplicateMapChance` | 🟡 | chance completion drops a copy of the run map; normal node, **not** a keystone |

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
| Double event | `DoubleEventChance` | 🟡 | player-stat that raises the bonus-content *count* (parallels `BONUS_CONTENT_CHANCE` relic stat) |
| Singular Focus (one per league) | perk set, `one_kind = "singular_focus"` | 🟡 | mutually exclusive; +50% weight to its event, −50% to the others |

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
