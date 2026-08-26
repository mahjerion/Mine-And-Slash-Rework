# Data-gen internals

Deep-dive detail for `## Data generation` in CLAUDE.md — read this when a datagen run behaved unexpectedly.

## One mod per run
`ExileRegistryUtil.MODID_TO_GENERATE_DATA` is a predicate over a **single** modid (`setCurrentRegistarMod` sets `x -> x.equals(modid)`); `ExileDatapackGenerator.generateAll` skips every entry whose `getRegistrationInfo().modid` fails it. Each mod calls `setCurrentRegistarMod` from its own `if (RUN_DEV_TOOLS)` block — whichever mod's flag is on claims the run, and if two are on, whichever was constructed last silently wins while the other emits nothing.

Per-mod flag and generator call:

| Mod | Flag | Generator call |
|---|---|---|
| `library_of_exile` | `CommonInit.RUN_DEV_TOOLS` — own flag, unreachable from any other mod's setting | `new LibDataGen().run(...)`; also prints "WARNING: Dev tools ON!" in chat |
| `mmorpg` | `MMORPG.RUN_DEV_TOOLS` | none directly — only sets the modid; generation comes from `DataGenHook` |
| `dungeon_realm` | `DungeonMain.RUN_DEV_TOOLS` | `DungeonDatabase.INSTANCE.runDataGen(...)` |
| `the_harvest` | `HarvestMain.RUN_DEV_TOOLS` | `HarvestDatabase.INSTANCE.runDataGen(...)` |
| `ancient_obelisks` | `ObelisksMain.RUN_DEV_TOOLS` | `ObeliskDatabase.generateJsons()` |

"WARNING: Dev tools ON!" only comes from `CommonInit` — its absence does **not** mean an mmorpg/addon datagen pass failed.

## Output location
Follows `FMLPaths.GAMEDIR` with `run/` rewritten to `src/generated/resources/` (`BaseDatapackGenerator.movePath`) — files land in whichever project you launched the client from. Regenerating `Library-of-Exile-Rework`-owned entries means running `runClient` from `Library-of-Exile-Rework/` itself, not the root project.

## Datapack check timing
Adding/renaming a field on a shared Library-of-Exile registry class (e.g. `RelicAffix`) restales every mod's JSON of that type at once — needs a separate regen pass per owning mod. `JsonExileRegistry.compareLoadedJsonAndFinalClass` round-trips each entry through Gson and demands tree equality (key order/indentation/line endings are free; a wrong/missing key or changed value fails it) — a mismatch shows as a red "[DATAPACK ERROR]" chat message plus a "Datapack Check Failed" diff in the log. The check runs at **world load**, generation at **player login just after** — so the errors a run reports describe state *before* that run's fix. Always do a second run to confirm. Log at `<project>/run/logs/latest.log`; failures greppable via `The file with id (\S+) is different after loading`.

## Why `runData` breaks things
`runData` registers the same providers via `GatherDataEvent`, but Forge datagen never fires `FMLCommonSetupEvent` (where `ExileEvents.EXILE_REGISTRY_GATHER` populates the content DB), so every provider emits zero files — then its `HashCache` treats existing output as stale and deletes all of `src/generated/resources`. That folder is gitignored/untracked (dropped from git in `b75833f5`, Dec 2023), so `git status` won't warn you; the only recovery is in-game regeneration. `build.gradle` adds it as a `sourceSets.main.resources` dir, so it must exist before `./gradlew build` produces a complete jar.
