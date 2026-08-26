# CLAUDE.md

Guidance for Claude Code when working in this repo.

## What this is
**Mine and Slash** — Minecraft **Forge 1.20.1** mod (id: `mmorpg`), a Path-of-Exile-style ARPG overhaul (gear rarities, affixes, stats, spells/skill gems, talent trees, maps/leagues, professions). Java 17, NeoForged `moddev.legacyforge` Gradle plugin. Uses Mixin, Curios, JEI, Player Animator.

## Build & run
Gradle wrapper (`./gradlew`/`gradlew.bat`). Dev env needs a **JetBrains Runtime (JBR)** JDK 17 toolchain (hot-swap via Enhanced Class Redefinition).

- `build` — full build (`reobfJar`)
- `runClient` — dev client
- `runClient2` — second client (`Dev2`) for multiplayer/sync testing
- `runServer` — dedicated server
- `publishMods` — CurseForge/Modrinth (needs `key.properties`/`modrinth_key.properties`)

No lint/test suite — correctness comes from mod-init error checks and running the game.

### Data generation — never `./gradlew runData`
Datapack JSON (stats, spells, gear, etc.) is generated from Java but only **in-game**, not via `runData`:

1. Set `MMORPG.RUN_DEV_TOOLS = true`.
2. `./gradlew runClient`, join a world (must be a client — the lang file only writes client-side).
3. On login, `DataGeneration.generateAll()` writes the lang file, datapack JSON (via `DataGenHook`), `modpack_dev_helper` dumps, curio JSONs, item models.
4. Set `RUN_DEV_TOOLS` back to `false`.

**Why never `runData`:** Forge datagen never fires `FMLCommonSetupEvent`, so the content DB is empty and every provider writes zero files — then its `HashCache` treats existing output as stale and **deletes all of `src/generated/resources`**. That folder is gitignored/untracked, so `git status` won't warn you, and in-game regeneration is the only recovery.

**Only one mod's `RUN_DEV_TOOLS` should be on per run** — each mod (`mmorpg`, `library_of_exile`, `dungeon_realm`, `the_harvest`, `ancient_obelisks`) claims the whole run for itself, and if two are on the one constructed last silently wins while the other emits nothing; regenerating a shared `Library-of-Exile-Rework` type needs a separate pass per owning mod. The stale-JSON check (`JsonExileRegistry.compareLoadedJsonAndFinalClass`) runs at world load, one step before that same login's generation would fix it — so always do a **second** run to confirm a fix actually landed. Full per-mod flag/generator table, output-path rules, and log-grep pattern: `design/datagen-internals.md`.

## Craft to Exile 2 modpack — live pack overrides
Day-to-day `mmorpg` tuning often happens **outside this repo** as openloader datapack overrides, which win over the mod's own generated JSON at runtime. Only touch this **when asked** — default to editing `aoe_data` + regenerating.

- **Working copy (edit here):** `C:\Users\Kelvin\curseforge\minecraft\Instances\Craft to Exile 2\` — datapack overrides under `config\openloader\data\cte_mns\data\` (per-registry-type folders mirroring `src/generated/resources`) and sibling `data\library_of_exile\`, `data\dungeon_realm\`, `data\ancient_obelisks\`; Forge configs under `defaultconfigs\*-server.toml`.
- **Live server — never edit:** `C:\Users\Kelvin\Documents\GitHub\Craft-to-Exile-2-Server\` — same paths, but the user copies client→server by hand; it lags the instance and isn't a git repo (no history to recover). Read-only, for checking what's actually live.
- Ignore other CurseForge instances, `curseforge\minecraft\Backups\`, and this repo's own `src\generated\resources`/`build\resources\main` when hunting for "the" pack.

## Submodules
Four git submodules via Gradle `includeBuild` (local edits picked up directly, no version bump needed): `Library-of-Exile-Rework` (core framework — most abstractions live here, not in `mmorpg`) and content addons `dungeon_realm`, `the_harvest`, `ancient_obelisks`. Init with `git submodule update --init`; versions pinned in `gradle.properties`; remotes point at `mahjerion` forks. `.claudeignore` gates whether submodule source is visible — remove its line there to edit one.

Library-of-Exile provides: the registry/content-database system `ExileDB`/addon `*Database`s are built on (`ExileRegistry`/`JsonExileRegistry`, the latter datapack-serializable and round-trip-checked on load); capabilities (`ICap`); a Forge-independent event bus (`ExileEvents`) addons use to decouple from each other; the shared map/instanced-dimension framework (`MapDimensionInfo`, `MapRegisterBuilder`) all three addons build their leagues on; the orb crafting-currency system (`orbs_of_crafting`); and mod-bootstrap helpers (`LibModConstructor`/`OrderedModConstructor`, `ApiForgeEvents`, `Packets`). Class-level map: `design/submodule-architecture.md`.

**Every mod (main + 3 addons) shares one skeleton** — registers an `OrderedModConstructor` subclass, exposes `RUN_DEV_TOOLS` (false in shipped builds) + `REGISTER_INFO` + its own `NETWORK` channel, and groups content into parallel `*Entries`/`*Words`/`*Commands`/`*LootTables`/`*Client`/`ComponentInit`/`*Database` classes.

Addons are instanced-map leagues on that framework:
- **`dungeon_realm`** — dungeon dimension (arenas, uber arenas, reward rooms), relics, identifiable map items; occasionally drops a map on chest loot. Variable per-dungeon room sizes: `design/variable-dungeon-room-size.md`.
- **`the_harvest`** — farming/harvest league, driven by farmable-block mining events.
- **`ancient_obelisks`** — obelisk structures with tiered mob stat scaling (`ObeliskMobTierStats`, via `ObeliskMobTierStatsMixin`) and `ObeliskRewardLogic`.

## Architecture (mmorpg)

- **Entry point:** `MMORPG.java` (`@Mod("mmorpg")`) wires `MnsConstructor`, capabilities, packets, event hooks, config, and the content-DB `init()`/`loadClass()` calls. `MMORPG.RUN_DEV_TOOLS` must be `false` in public builds.
- **Content DB:** query via `database/registry/ExileDB.java` (`ExileDB.Spells()`, etc.); types/order/sync timing in `ExileRegistryTypes`. Definitions live in `aoe_data/database/**` (dev-only Java source that emits datapack JSON — treat as source, not runtime). `database/data/**` holds the runtime classes those entries instantiate. After editing `aoe_data`, regenerate (see Data generation above) — never hand-edit the JSON.
- **Entity/player data:** capabilities in `capability/entity/EntityData.java` (stats, rarity, affixes, resources, gear — mobs and players both) and `capability/player/PlayerData.java` (+ `world`/`chunk` caps). **Access via `uncommon/datasaving/Load.java`** (`Load.Unit()`, `Load.player()`, etc.), not capabilities directly. NBT POJOs in `saveclasses/**`; sync via `DirtySync`/`INeededForClient`.
- **Stats & combat:** scaling in `database/data/stats/**`, calc in `saveclasses/unit/stat_calc/**` + `uncommon/stat_calculation/**`. Damage/effect flow via `uncommon/effectdatas/**` (`DamageEvent`, `EventBuilder`) and the newer `effectdatas/rework/**` (`StatEffect`/`StatCondition`); hooked from `event_hooks/**` and `mixins/**`.
- **Mixins:** `mixins/` (config `mmorpg.mixins.json`); `mixin_ducks/` = duck accessors, `mixin_methods/` = method targets; access wideners in `accesstransformer.cfg`.
- **Other:** `gui/` screens/HUD, `maps/` map-league system, `database/data/spells/**` spell/skill-gem components, `config/forge/**` configs, `compat/` third-party mod compat.

## Conventions
- Registered-object prefix is `Slash*`/`Exile*`; runtime mod id is always `mmorpg`.
- Never hand-edit generated datapack JSON or the lang file — edit `aoe_data` Java and regenerate.
- `assets/mmorpg/modpack_dev_helper/*.txt` are generated dumps for modpack devs — outputs, not inputs.
