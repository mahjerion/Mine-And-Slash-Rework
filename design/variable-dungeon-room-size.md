# Variable dungeon room size — implementation notes

Lets a dungeon map (`dungeon_realm:dungeon`) be built from rooms larger than one chunk, declared
per-dungeon in the datapack. Target case: **32×32-block rooms** (any height up to 48). Defaults keep
every existing dungeon byte-identical. Arenas / uber arenas / reward rooms
(`SimplePrebuiltMapStructure`) are out of scope — this only touches the modular room-grid dungeons.

Core idea: **rooms stay a grid of cells; a cell just becomes N×N chunks instead of 1×1.** The 20×20
cell grid, the room-connection topology, and the building-block system are all unchanged — only the
chunk→cell mapping and the placement call changed.

## The constraint that shaped everything

`MapChunkGenerator.buildSurface` runs at `ChunkStatus.SURFACE`, whose `WorldGenRegion` is built with
`writeRadiusCutoff = 0`. **Only the chunk currently being generated is writable** — you cannot place a
32×32 room in one call, because 3 of its 4 chunks are outside the writable region.

Fix: author each room as **one big `.nbt`** and place it **once per chunk it covers**, each call clipped
to that chunk. Vanilla already supports this — `StructurePlaceSettings.setBoundingBox(...)` clips
`StructureTemplate.placeInWorld` for both blocks and entities (the same mechanism
`TemplateStructurePiece.postProcess` uses). The room is anchored at its own min corner every time; the
clip box decides which quadrant actually gets written.

## Datapack knobs (`DungeonData`)

Two independent axes, both now settable per-dungeon in the dungeon JSON. Both default to the old
behaviour, so untouched dungeon JSON deserializes exactly as before.

| Field | Default | Meaning |
|---|---|---|
| `room_size` | `16` | **Footprint.** Blocks per side of every room, X/Z. Must be a multiple of 16, ≤ `16 * MAX_ROOM_CHUNKS` (64). |
| `min_rooms` | `0` | **Count.** Overrides `DungeonConfig.MIN_MAP_ROOMS`. `0` = use the global config. |
| `max_rooms` | `0` | Overrides `DungeonConfig.MAX_MAP_ROOMS`. `0` = use the global config. |

- **Size and count are separate.** `room_size` is the footprint; `min/max_rooms` is how many rooms. A
  32-wide room is 6× the volume, so `min/max_rooms` lets a big-room dungeon ask for fewer rooms without
  touching the global config that every 16-wide dungeon still uses.
- **Global config values were not changed.** `DungeonConfig.MIN_MAP_ROOMS` / `MAX_MAP_ROOMS` (16–20)
  remain the default for any dungeon that doesn't override them.
- Builder helpers: `Dungeon.Builder.roomSize(int)` and `.rooms(min, max)`.

### Height

Height is **independent** of `room_size` and does **not** have to be uniform across a dungeon's rooms.

- Existing rooms are `16×20×16` (20 tall). New rooms can be any height per room.
- Cap is **48** (`MapStructure.getStructureHeight()`) — the band `isInside(pos)` treats as inside the
  map (`[getSpawnHeight(), getSpawnHeight()+48]`, i.e. `[50, 98]`). Each room is height-checked
  individually; taller than 48 is a fatal reject.
- All rooms anchor at the same floor (`getSpawnHeight()` = 50), so mixed-height rooms have flush floors
  and varying ceilings. Doors line up as long as their openings sit at a consistent floor-relative
  position — the same discipline the existing 16-wide rooms already follow.

## How a chunk resolves to a room (`BuiltDungeon`)

New `getPlacementForChunk(struc, chunkPos, roomChunks)` → `RoomPlacement { room, originChunk, subX, subZ }`.

- `cell   = getMiddle() + Math.floorDiv(relative, roomChunks)`
- `sub    = Math.floorMod(relative, roomChunks)`
- `origin = chunkPos − sub`

`floorDiv`/`floorMod` are **required** — `relative` is negative on half the grid, and `/` and `%`
truncate toward zero, which would fold cell `-1` into cell `0`. `getRoomForChunk`/`hasRoomForChunk` now
delegate here; with `roomChunks == 1` the result is bit-identical to the old code.

`getTotalChunkCount()` = `amount * roomChunks²` — cells × chunks-per-cell.

## Placement (`DungeonRoomPlacer`)

`generateStructure`:
1. Resolve the placement once (collapsing the old double `hasRoomForChunk` + `getRoomForChunk`, each of
   which re-derived the start 2×).
2. Barrier cells: the 16×16 barrier is tiled unclipped over each sub-chunk.
3. Real rooms: anchor at `originChunk`, clip to the current chunk (`clip == null` when `roomChunks == 1`,
   preserving the exact old path).

`generatePiece` gained `clip`, `fireDataBlockEvents`, `expectedSize`, `maxHeight`; the old 5-arg
signature is kept as a delegating overload.

- **Data-block events fire only on the origin chunk** (`subX == 0 && subZ == 0`). This is both a
  correctness fix — the `DUNGEON_DATA_BLOCK_PLACED` consumer increments per-map mob-spawn counts, and
  firing it in all 4 chunks would inflate them 4× — and a perf offset: the two full-template
  `filterBlocks` scans now run once per room instead of once per chunk.
- Size/height guards warn (de-duped via a `static Set<ResourceLocation>`, since placement repeats per
  chunk) or fatally reject.

**Cost:** a 2×2 room walks its block list 4× instead of 1×, offset by the `filterBlocks` scans dropping
to once per room. Actual `setBlockState` work is unchanged and dominates.

## Instance spacing (`DungeonMapStructure`)

The 20×20 cell grid stays; bigger rooms make a bigger dungeon (in blocks/ground), not a dungeon with
fewer rooms. A 32-wide dungeon's grid is 40 chunks across, so instance spacing had to grow.

```
GRID_CELLS           = 20
MAX_ROOM_CHUNKS      = 4                       // room_size ≤ 64
MAX_GRID_SPAN_CHUNKS = 20 * 4 = 80
DUNGEON_LENGTH       = 90                       // was 30
START_OFFSET         = 80/2 + 1 = 41           // was 11
```

`INTERNALgetStartChunkPos` resolves the start from chunk coords **before** the dungeon (and thus its
room size) is known, so one constant pair must cover every size — sized for the largest supported room
so it never has to change again. Also switched `%` → `Math.floorMod`: the old `11 - (chunk % LENGTH)`
was non-idempotent for negative chunk coords (`-31 → -19 → 11`), which the commented-out consistency
check in `MapStructure.getStartChunkPos` was guarding against; it stayed dormant only because instance
coords were never negative.

### ⚠ Save compatibility

Changing `DUNGEON_LENGTH` / `START_OFFSET` **re-grids where instances live.** Dungeons already generated
in an existing save align to the old grid while new chunks align to the new one, and in-progress
`DungeonMapData` lookups stop resolving. **The dungeon dimension must be wiped** when updating an
existing world. This is a one-time cost — `MAX_ROOM_CHUNKS = 4` means adding a 48- or 64-block dungeon
later needs no further re-grid. Room-size and room-count fields on their own are additive and need no
wipe; only the spacing change does.

## Bundled fixes (independent of room size)

- **`DungeonEvents` chunk accounting** — bonus-content spawn pacing used `builtDungeon.amount` (cells)
  against `processedChunks` (chunks). With 2×2 rooms it thought the dungeon was exhausted 4× early and
  force-spawned at 100%. Now uses `getTotalChunkCount()`.
- **`MapStructureCounter` allocator** — was a straight line along +Z (`x=1, z++`), so the Nth map sat N
  spacings out and long-lived servers crawled toward the world border. Now walks a 64-wide band, growing
  Z 64× slower. Save-compatible: resumes past any already-issued cell, no serialization change.
- **`DungeonStructure.builtDungeonCache`** — was an unbounded `ConcurrentHashMap`, retaining a full 20×20
  `BuiltRoom` grid per instance for the server's lifetime. Now a 32-entry LRU (`synchronizedMap` +
  `removeEldestEntry`). Eviction is safe: builds are deterministic from start-chunk + world seed.

## Files touched

**Library of Exile** (`library_of_exile/dimension/structure/dungeon/`, plus `worlddata/`)
- `DungeonData` — `room_size` / `min_rooms` / `max_rooms`, `getRoomSizeInChunks()`, `MAX_ROOM_CHUNKS`, validation
- `BuiltDungeon` — `RoomPlacement`, `getPlacementForChunk`, `getTotalChunkCount`
- `DungeonRoomPlacer` — clipped placement, once-per-room data events, size/height guards
- `DungeonBuilder` — `getRoomChunks()`
- `DungeonStructure` — bounded LRU cache
- `worlddata/MapStructureCounter` — band walk

**dungeon_realm**
- `structure/DungeonMapStructure` — spacing constants, `floorMod`, per-dungeon room count
- `database/dungeon/Dungeon` — `Builder.roomSize(int)`, `.rooms(min, max)`
- `main/DungeonEvents` — `getTotalChunkCount()`

## Build / data-gen notes

- The three addons compile against a **git-tracked** `libs/Library_of_Exile-1.20.1-<ver>.jar`, not the
  composite build (`exile_lib_dep.gradle` uses `flatDir`). After changing library code, rebuild the
  library jar (`./gradlew :Library-of-Exile-Rework:build`) and copy it into each addon's `libs/`, or the
  addons compile against stale classes.
- Adding `DungeonData` fields makes `JsonExileRegistry.compareLoadedJsonAndFinalClass` log a
  "Datapack Check Failed" warning for shipped dungeon JSON that lacks them. Regenerate via the dungeon
  data-gen flow (gated behind `DungeonMain.RUN_DEV_TOOLS`, runs in-game on login) — runtime behaviour is
  unaffected either way, the fields just default.

## Authoring a big-room dungeon

1. Build rooms at exactly `room_size × room_size` in X/Z (a smaller footprint places with a gap and
   warns; larger is a fatal reject). Height free per room, ≤ 48.
2. One room minimum per `RoomType` — entrance, four_way, straight_hallway, curved_hallway,
   triple_hallway, end — or `DungeonData.checkValidity` rejects the dungeon.
3. Keep door openings at a consistent floor-relative position across all room types so cells connect.
4. Place them under `data/library_of_exile/structures/dun/<folder>/<type>/`, set `room_size` (and
   optionally `min_rooms`/`max_rooms`) on the dungeon, regenerate data.

## Verifying

- **`room_size = 16` must stay byte-identical** — `roomChunks == 1` ⇒ `clip == null`, data events always
  fire, `floorDiv`/`floorMod` reduce to `/`/`%`.
- **32-wide:** rooms seamless across all 4 chunk boundaries, including **rotated** rooms (rotation offsets
  the anchor by `size − 1`); barrier cells fully seal a 2×2; baked-in entities appear once, not 4×.
- `/<mns> report map_bug` from any of a big room's 4 chunks names that room.
- Complete a 32-wide map: completion % and bonus-content pacing behave sanely (protected by the
  once-per-room events and `getTotalChunkCount`).
- Wipe the dungeon dimension before testing on a pre-existing save (spacing changed 30 → 90).
