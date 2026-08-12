# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**Mine and Slash** is a large Minecraft **Forge 1.20.1** mod (mod id: `mmorpg`) that turns Minecraft into a Path-of-Exile-style hack-and-slash ARPG (gear rarities, affixes, stats, spells/skill gems, talent trees, maps/leagues, professions). Java 17. Built with the NeoForged `moddev.legacyforge` Gradle plugin. Uses Mixin, Curios, JEI, and Player Animator.

## Build & run commands

Use the Gradle wrapper (`./gradlew` / `gradlew.bat`). The dev environment expects a **JetBrains Runtime (JBR)** JDK 17 toolchain — runs are configured to force JBR and enable Enhanced Class Redefinition for hot-swapping (add/change methods while running).

- `./gradlew build` — full build (produces the reobfuscated jar via `reobfJar`).
- `./gradlew runClient` — launch a dev Minecraft client.
- `./gradlew runClient2` — second client (username `Dev2`) for multiplayer/sync testing.
- `./gradlew runServer` — dedicated server.
- `./gradlew publishMods` — publishes to CurseForge/Modrinth (needs `key.properties` / `modrinth_key.properties`).

There is no separate lint or unit-test suite; correctness is validated at mod init via error-check passes and by running the game.

### Data generation — do NOT use `./gradlew runData`

Datapack JSON for content (stats, spells, gear, etc.) is generated from Java code, not hand-written, but **`runData` is not how you generate it.** Data gen runs *in-game*:

1. Set `MMORPG.RUN_DEV_TOOLS = true` (`MMORPG.java`).
2. `./gradlew runClient` and join any world. It must be a **client** — the lang file is written under `DistExecutor.safeRunWhenOn(Dist.CLIENT, ...)`, so `runServer` won't produce it.
3. On `PlayerLoggedInEvent`, `LifeCycleEvents` runs `DataGeneration.generateAll()` — the `mmorpg` lang file, `DataGenHook`, the `modpack_dev_helper` txt dumps, curio JSONs, and item models. `DataGenHook` is what loops `ExileRegistryType.getAllInRegisterOrder()` and runs each type's datapack generator, so the registry content JSON comes from there.
4. Set `RUN_DEV_TOOLS` back to `false`.

#### One mod per run — a client run does *not* generate all four mods

`ExileRegistryUtil.MODID_TO_GENERATE_DATA` is a predicate over a **single** modid (`setCurrentRegistarMod` sets `x -> x.equals(modid)`), and `ExileDatapackGenerator.generateAll` skips every entry whose owning `getRegistrationInfo().modid` fails it. Each mod calls `setCurrentRegistarMod` from inside its own `if (RUN_DEV_TOOLS)` block, so **whichever mod has its flag on claims the run** — and if two are on, the one constructed last silently wins while the other emits nothing.

Each mod also drives its own generator, and they are not symmetric:

| Mod | Flag | Generator call |
|---|---|---|
| `library_of_exile` | `CommonInit.RUN_DEV_TOOLS` — **its own flag**, unreachable from any other mod's setting | `new LibDataGen().run(...)`, also prints "WARNING: Dev tools ON!" in chat |
| `mmorpg` | `MMORPG.RUN_DEV_TOOLS` | none in that block — only sets the modid; generation comes from `DataGenHook` above |
| `dungeon_realm` | `DungeonMain.RUN_DEV_TOOLS` | `DungeonDatabase.INSTANCE.runDataGen(...)` |
| `the_harvest` | `HarvestMain.RUN_DEV_TOOLS` | `HarvestDatabase.INSTANCE.runDataGen(...)` |
| `ancient_obelisks` | `ObelisksMain.RUN_DEV_TOOLS` | `ObeliskDatabase.generateJsons()` |

Because "WARNING: Dev tools ON!" comes only from `CommonInit`, **its absence does not mean datagen failed** — an mmorpg or addon pass never prints it.

Output location follows `FMLPaths.GAMEDIR` with `run/` rewritten to `src/generated/resources/` (`BaseDatapackGenerator.movePath`), so files land in **whichever project you launched the client from**. Regenerating `library_of_exile`-owned entries means running the library's *own* `runClient` from `Library-of-Exile-Rework/`; the root client would write them to the wrong project.

**Consequence:** adding or renaming a field on a shared Library-of-Exile registry class (e.g. `RelicAffix`) restales *every* mod's JSON of that type at once, so it needs a separate pass per owning mod. `JsonExileRegistry.compareLoadedJsonAndFinalClass` round-trips each entry and demands byte-equality, and a mismatch shows as a red "[DATAPACK ERROR]" chat message on login plus a "Datapack Check Failed" diff in the log. Note the check runs at **world load** while generation runs at **player login just after**, so the errors a run reports describe the state *before* that run fixed anything — always do a second run to confirm a fix. The log (`<project>/run/logs/latest.log`) is the source of truth; the failure list is greppable via `The file with id (\S+) is different after loading`.

`runData` registers the same providers via `GatherDataEvent`, but Forge datagen never fires `FMLCommonSetupEvent` — which is where `ExileEvents.EXILE_REGISTRY_GATHER` populates the content DB — so every provider emits **zero** files. Worse, its `HashCache` then treats the existing output as stale and **deletes all of `src/generated/resources`**.

`src/generated` is gitignored and untracked (dropped from git in `b75833f5`, Dec 2023), so `git status` will *not* warn you if that happens. The files are purely local build artifacts; the only recovery is the in-game regeneration above. `build.gradle` adds `src/generated/resources` as a `sourceSets.main.resources` dir, so they must exist before `./gradlew build` produces a complete jar.

## Multi-module / submodule layout (important)

This repo depends on four sibling libraries that are **git submodules** included as Gradle `includeBuild` composite builds — code changes in them are picked up directly by the main build (no need to bump versions or reinstall for local edits):

- `Library-of-Exile-Rework` (`com.robertx22:Library-of-Exile-Rework`) — the core framework this mod is built on. **Most core abstractions live here, not in the mod.** See "Submodule architecture" below.
- `dungeon_realm`, `the_harvest`, `ancient_obelisks` — self-contained content addons, each its own `@Mod`.

Submodules must be initialized (`git submodule update --init`) before the project will build. Versions are pinned in `gradle.properties` (`exile_library_version`, etc.) and must be kept in sync with the submodule contents when publishing. Submodule remotes point at the `mahjerion` forks. Note: in `settings.gradle`, `ancient_obelisks` uses a `dependencySubstitution` to map the hyphenated module name `com.robertx22:ancient-obelisks` onto its included build.

`.claudeignore` gates whether the submodule source is visible to tooling; to edit a submodule make sure its line is removed there.

## Submodule architecture

### Library of Exile (the framework)
Package root `com.robertx22.library_of_exile` (plus `com.robertx22.orbs_of_crafting`, bundled in the same jar). Everything below is what the main mod and the three addons are built on — when a core abstraction seems missing from `mmorpg`, look here.

- **Registry / content database** (`registry/`) — the system `mmorpg`'s `ExileDB` and every addon's `*Database` are built on.
  - A content type implements `ExileRegistry<C>` (has a `GUID()`, a weight, and an `ExileRegistryType`). Implementing `JsonExileRegistry<T>` instead makes entries **datapack-serializable** — they ship as generated JSON and are reloadable. `JsonExileRegistry.compareLoadedJsonAndFinalClass` round-trips each entry through Gson on load and logs a loud "Datapack Check Failed" diff if the class and its JSON disagree (wrong/missing field names, types) — watch the log for these.
  - `ExileRegistryType` defines a type's `id`, load `order`, and `SyncTime` (when it syncs to clients). `Database` / `ExileRegistryContainer` hold the registered entries per type. `LibDatabase` is the Library's own content accessor (leagues, affixes, relics, mob lists, map data blocks, map finish rarities).
- **Capabilities** (`components/`) — `ICap extends ICapabilitySerializable<CompoundTag>`; `syncToClient` pushes via `Packets` → `SyncPlayerCapToClient`. All the per-entity/-chunk/-map/-world data classes in the mod and addons implement this.
- **Custom event bus** (`events/base/ExileEvents`) — a Forge-independent event system (`ExileEventCaller` + `EventConsumer`) used to decouple cross-mod hooks: damage phases (`DAMAGE_BEFORE_CALC` / `AFTER_CALC` / `BEFORE_APPLIED`), `MOB_DEATH`, `MOB_KILLED_BY_PLAYER`, `ON_CHEST_LOOTED`, mining events, `AFTER_DATABASE_LOADED`, `ON_PLAYER_LOGIN`, and `CHECK_IF_DEV_TOOLS_SHOULD_RUN` (how `LibraryOfExile.runDevTools()` asks the host mod whether dev tools are on). Addons register listeners here rather than depending on each other directly.
- **Map / league dimension framework** (`dimension/` + `config/map_dimension/`) — the shared machinery for the "enter an instanced map dimension" leagues. A mod describes its dimension with a `MapDimensionInfo` (dimension key, primary `MapStructure`, extra content structures, a `MobValidator`, defaults) and registers it via `MapRegisterBuilder`, supplying a chunk-gen consumer (`MapChunkGenEvent`). `MapDimensions` answers `isMap(level)`; there's a dimension/folder wipe feature. This is why the three addons look nearly identical structurally.
- **Crafting currency** (`orbs_of_crafting`) — the "orb" currency system: `Orbs`, `Modifications`, `Requirements`, `OrbEdits` (registered as key holders in `LibModConstructor`). Orbs apply modifications/requirements to items. The main mod wires into it via `OrbAddonEvents.register()`.
- **Mod bootstrap helpers** (`main/`) — `LibModConstructor` / `OrderedModConstructor` (deterministic multi-mod registration order), `ApiForgeEvents.registerForgeEvent(...)` (the standard way all these mods add Forge listeners), `ExileLog`, `Packets`, data-gen bases (`LibDataGen`, `LibLootTables`).

### Common addon pattern
The main mod **and** each addon follow the same skeleton, so once you know one you know all four. Each `@Mod` class:
- registers an `OrderedModConstructor` subclass (`MnsConstructor`, `DungeonModConstructor`, `HarvestModConstructor`, `ObeliskModConstructor`) on the mod event bus;
- exposes `public static boolean RUN_DEV_TOOLS` (data-gen + registry recording; **false in shipped builds**), a `ModRequiredRegisterInfo REGISTER_INFO`, its own `SimpleChannel NETWORK`, and an `id(String)` helper;
- groups its pieces into parallel `*Entries` (deferred registers), `*Words` (localization), `*Commands`, `*LootTables`, `*Client`, `ComponentInit` (capability attach), and `*Database` (content) classes;
- runs data generation only when its `RUN_DEV_TOOLS` is on (on player login, not via `runData`).

### The three content addons
Each is an instanced-map league built on the framework above (its own dimension, structures, entity + map/world capabilities, mob validator, config, and content database):
- **`dungeon_realm`** — the `dungeon_realm:dungeon` map dimension with arenas, uber arenas, and reward rooms; relics; item mods/requirements; and identifiable dungeon-map items (`IdentifiableItems`). Hooks `ON_CHEST_LOOTED` to occasionally drop a dungeon map in the overworld. Dungeon maps support per-dungeon room sizes larger than one chunk (datapack `room_size`) — see `design/variable-dungeon-room-size.md`.
- **`the_harvest`** — a farming/harvest league dimension driven by mine-farmable-block events; harvest map items.
- **`ancient_obelisks`** — obelisk structures whose mobs get tiered stat scaling (`ObeliskMobTierStats`, applied via the main mod's `ObeliskMobTierStatsMixin`) and `ObeliskRewardLogic`; item mods/requirements.

## Architecture

### Mod entry point
`mmorpg/MMORPG.java` (`@Mod("mmorpg")`) is the constructor that wires everything: registers the `MnsConstructor`, capabilities, packets (`C2SPacketRegister` / `S2CPacketRegister`), Forge/Exile event hooks, config specs, and kicks off all the `init()`/`loadClass()` static initializers that populate the content database. `SlashRef` holds the mod id and `ResourceLocation` helpers.

`MMORPG.RUN_DEV_TOOLS` **must be `false` for public builds** — it enables the data-generators and registry-recording tooling. `RUN_DEV_TOOLS_REMOVE_WHEN_DONE` mirrors it as a self-reminder guard.

### The content database (`ExileRegistry`)
Content types (stats, spells, gear types, affixes, gems, runes, rarities, maps, professions, prophecies, omens, talent trees, etc.) are registered into an in-memory database from the **Library of Exile** framework:

- Query the database through `database/registry/ExileDB.java` — the central accessor for every registered content type (`ExileDB.Spells()`, `ExileDB.GearRarities()`, etc.). `ExileRegistryTypes` / `ExileRegistryType` define the registry types and their load order & sync timing.
- **Content definitions live in `aoe_data/database/**`** (one subpackage per type). These build the registry entries in Java. Per `aoe_data/package-info.java`, this package is intended to run **only in the dev environment** — it emits datapack JSON via data-gen, which is what actually ships. Treat `aoe_data` as "the source that generates data," and `database/data/**` as the runtime classes those entries instantiate.
- Because content is datapack-driven, **after changing anything in `aoe_data`, regenerate `src/generated/resources`** by logging into a dev client with `RUN_DEV_TOOLS = true` — see "Data generation" above. Not `runData`.

### Entity/player data (capabilities)
Per-entity RPG state is stored in Forge capabilities built on Exile's `ICap`:

- `capability/entity/EntityData.java` — the big one: a `LivingEntity`'s `Unit`/stats, rarity, affixes, resources (health/mana/energy), gear cache, etc. Applies to both mobs and players.
- `capability/player/PlayerData.java`, plus `world`, `chunk` capabilities.
- **`uncommon/datasaving/Load.java` is the canonical accessor** — use `Load.Unit(entity)`, `Load.player(player)`, `Load.worldData(level)`, `Load.chunkData(chunk)` rather than fetching capabilities directly.
- `saveclasses/**` holds the NBT-serializable POJOs (via Exile's `LoadSave`/AutoGson) that capabilities persist — e.g. `saveclasses/unit/Unit.java`, `GearItemData`, stat data. Capabilities sync to the client through packets; look for `DirtySync` / `INeededForClient`.

### Stats & combat
- Stats and their scaling/derivation live under `database/data/stats/**`; stat calculation in `saveclasses/unit/stat_calc/**` and `uncommon/stat_calculation/**`.
- Damage/effect flow uses "effect data" events: `uncommon/effectdatas/**` (`DamageEvent`, `EventBuilder`, and the newer `effectdatas/rework/**` with `StatEffect`/`StatCondition`). Combat is hooked via `event_hooks/**` and `mixins/**`.

### Mixins
Vanilla patches are in `mixins/` (config: `src/main/resources/mmorpg.mixins.json`, package `com.robertx22.mine_and_slash.mixins`). `mixin_ducks/` are duck-interface accessors and `mixin_methods/` are helper method targets. Access wideners are in `src/main/resources/META-INF/accesstransformer.cfg`.

### Other notable packages
- `gui/` — screens (map device, atlas, character sheet, etc.) and HUD overlays.
- `maps/` — the map/dungeon league system (`MapData`, `MapEvents`).
- `database/data/spells/**` — the spell/skill-gem component system (spells are data-defined from components/conditions/map_fields).
- `config/forge/**` — Forge config specs (client, server, compat).
- `compat/` — third-party mod compatibility.

## Conventions

- Prefix for most registries/registered objects and many classes is `Slash*` or `Exile*`; the runtime mod id string everywhere is `mmorpg`.
- Don't hand-edit generated datapack JSON under `src/generated/resources` or the generated lang file — change the Java source in `aoe_data` and re-run data-gen (in-game, see "Data generation"). `MMORPG.createMnsLangFile()` / `CreateLangFile` build the lang file.
- The `src/main/resources/assets/mmorpg/modpack_dev_helper/*.txt` files are human-readable dumps of registered content (affixes, stats, currency, etc.) for modpack devs — they are outputs, not inputs.
