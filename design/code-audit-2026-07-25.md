# Code audit — 2026-07-25

Review of the Atlas / bonus-encounter / currency feature push (commits `53295bc3..a1605ae8`, ~35
commits, 314 files, +9.4k lines) covering the Atlas map & passive tree, the three new bonus-map
encounters (Strongbox, Imprisoned Monster, Shrine), the ~45 new loot stats, prophecy rerolls, and
the new currencies.

All 12 findings below were fixed in the same pass. Status column is the state as of this document.

| #  | Finding | Severity | Status |
|----|---------|----------|--------|
| 1  | Unguarded `getList().get(0)` on the Atlas layout registry | High | Fixed |
| 2  | `tryAcceptReward` `.get()` on an empty Optional | High | Fixed |
| 3  | Imprisoned Monster paid out for free | High | Fixed |
| 4  | Strongbox destroyed its own loot when nobody was in range | Medium | Fixed |
| 5  | Strongbox scaled difficulty and reward off different players | Medium | Fixed |
| 6  | Shrine sent ~200 particle packets per activation | Medium | Fixed |
| 7  | Global `LivingDeathEvent` hook with no early-out | Medium | Fixed |
| 8  | `EntityConfig` cache never invalidated on `/reload` | Medium | Fixed |
| 9  | `meetsRequirement` NPE on an unregistered rarity | Medium | Fixed |
| 10 | Atlas layout parsed before its nodes were registered | Low (latent) | Fixed |
| 11 | Pending-atlas-screen flag was sticky | Low | Fixed |
| 12 | Caged boss's rerolled affixes never applied to its stats | Medium | Fixed |

A second round followed from four bugs observed in playtesting. Findings 13–16 below.

| #  | Finding | Severity | Status |
|----|---------|----------|--------|
| 13 | Talent/ascendancy tree lag: a new `RenderType` per texture per frame | High | Fixed — needs playtest (pre-existing, not caused by this push — see Attribution) |
| 14 | Talent/ascendancy tree draw order decided by hash iteration order | High | Fixed — needs playtest (pre-existing; likely made visible by 15b) |
| 15 | Search bar drawn under the tree | Medium | Fixed — needs playtest |
| 16 | Atlas node not credited on map completion (server only) | High | Fixed — needs playtest |

---

## 1. Unguarded `getList().get(0)` on the Atlas layout registry — High

`AtlasNodeLayout.shouldGenerateJson()` returns `false`, so `atlas_map.json` exists only as a
hand-authored file under `src/main/resources/data/mmorpg/mmorpg_atlas_layout/`. Three call sites
indexed straight into the registry list:

- `gui/screens/atlas_map/AtlasMapScreen.java` (client, on screen open)
- `saveclasses/atlas/AtlasData.java` — `markCompleted`
- `addons/dungeon_realm/DungeonAddonEvents.java` — pinnacle unlock check

A datapack that shadows or omits that file leaves the registry empty and every one of these throws
`IndexOutOfBoundsException` — including on the map-completion path, which runs for every player on
every cleared map.

**Fix:** added `AtlasNodeLayout.mainCalcData()`, which falls back to `EMPTY.calcData` (blank maps,
`getConnectedIds` returns `Set.of()`). All three call sites now route through it. An absent layout
degrades to "Atlas map renders as nothing unlocked" instead of crashing.

## 2. `PlayerProphecies.tryAcceptReward` — `.get()` on an empty Optional — High

```java
ProphecyData data = rewardOffers.stream().filter(...).findAny().get();
if (data != null) {   // unreachable — get() already threw
```

The line predates this push, but **the new reroll feature made it reachable**: `tryReroll` →
`regenerateNewOffers` replaces `rewardOffers` wholesale, so a client whose Prophecy screen still
shows pre-reroll offers sends a uuid the server no longer holds → `NoSuchElementException` inside
the packet handler.

**Fix:** `.orElse(null)`, which makes the existing null check do its job.

## 3. Imprisoned Monster paid out its guaranteed reward for free — High

`ImprisonedMonsterBlock.monsterDead` polled `level.getEntity(uuid)` and treated "not found" as
"dead". `StrongboxBE`'s own header comment documents exactly why that's wrong — the strongbox was
already converted to `LivingDeathEvent` counting, this encounter wasn't. Two paths to a free reward:

- A captive leaving the loaded area (e.g. the owner uses the new boss teleport while another player
  keeps the block's chunk ticking) reads as dead.
- If `spawnMonster` released nothing (`type.create()` not a `Mob`), `activated` was still set to
  `true` with an empty UUID list, so `monsterDead` returned `true` on the very next tick.

**Fix:**
- Added `isImprisonedMonster` / `imprisonedMonsterPos` to `DungeonEntityData` (dungeon_realm
  submodule), mirroring the strongbox fields. Gson-serialized, so backward compatible.
- `ImprisonedMonsterBE` now persists `spawnedCount` + `monstersRemaining` instead of a UUID list,
  with a legacy branch that derives both from the old `monsters` list so already-activated blocks in
  existing worlds neither soft-lock nor pay out instantly.
- The `LivingDeathEvent` hook in `DungeonAddonEvents` decrements `monstersRemaining`.
- `spawnMonster` returns the spawn count; `use()` only arms the encounter when it's ≥ 1.

## 4. Strongbox destroyed its own loot — Medium

`unlock()` skipped the entire reward block when `getNearestPlayer(..., 48, ...)` returned null, but
still ran `level.removeBlock(pos, false)`. A player dying or leaving as the last guardian fell lost
the whole payout.

**Fix:** early `return` when there's no recipient — the box stays standing and retries on a later
tick. Same treatment applied to `ImprisonedMonsterBlock.reward`.

## 5. Strongbox scaled difficulty and reward off different players — Medium

Guardian toughness read the *activating* player's stats at spawn time; every reward stat read
`getNearestPlayer` at unlock time. In a party those are different players, so one player's Atlas
stats could make the fight harder while another's rolled the loot.

**Fix:** `StrongboxBE` persists `activatorId`; `resolveRecipient` prefers the opener when they're
alive and within range, falling back to nearest player otherwise.

## 6. Shrine particle packet spam — Medium

`spawnBuffWave` called `level.sendParticles` once per point, `max(16, radius * 24)` times — roughly
200 separate packets to every tracking player per activation. The sampling was also wrong: `x` used
`sin(phi)cos(theta)` while `z` used only `cos(phi)`, putting points on a shell rather than the disc
the comment described.

**Fix:** a single `sendParticles` call with a count and an xyz gaussian spread of `radius / 3`
(~99.7% inside the real buff radius).

## 7. Global `LivingDeathEvent` hook with no early-out — Medium

The hook fires for every living entity death in every dimension and immediately called
`DungeonEntityCapability.get(mob)`, which ends in `.orElse(new DungeonEntityCapability(entity))` —
a throwaway capability + data allocation on every miss.

**Fix:** `MapDimensions.isMap(mob.level())` guard first. Both encounters only exist inside map
dimensions.

## 8. `EntityConfig` cache never invalidated — Medium

`EntityData.getEntityConfig()` (added this push as a hot-path optimisation) cached forever. The
config resolves purely from entity type so it's correct per entity, but a `/reload` swaps the
registry entries while every already-loaded entity keeps the old object.

**Fix:** `EntityData.ENTITY_CONFIG_CACHE_GEN` generation counter, bumped from
`DatabaseCaches.resetCaches()` (already wired to `AFTER_DATABASE_LOADED`). Caches re-resolve lazily
rather than needing a walk over every live entity.

## 9. `meetsRequirement` NPE — Medium

`ExileDB.GearRarities().get(node.min_rarity).map_tiers.min` in `DungeonAddonEvents` NPEs if a node
names a rarity that isn't registered — on the map-completion path.

**Fix:** null-guarded; an unresolvable rarity is treated as "no rarity requirement".

## 10. Atlas layout parsed before its nodes existed — Low (latent)

`ATLAS_NODE_LAYOUT` was registered at order **20**; dungeon_realm's `ATLAS_NODE` is order **52**.
`AtlasGridPoint` resolves each grid cell via `AtlasNodes().isRegistered(id)` during the layout's
`onLoadedFromJson`, and both the server datapack load (`RegistryPackets.registerAll`) and the client
login sync (`Database.sendPacketsToClient`) walk registries in ascending order.

This did **not** break in practice, because `DungeonAtlasNodes` is an `ExileKeyHolder` that
code-registers its nodes at mod init, long before any datapack pass. But it meant a node added
*purely* by datapack JSON — with no Java holder — would be invisible to the grid on both sides.

**Fix:** moved `ATLAS_NODE_LAYOUT` to order **53**, with a comment recording the constraint.

> **Watch this:** if dungeon_realm ever renumbers `ATLAS_NODE`, this ordering must move with it.
> The failure is silent — an empty grid, no exception.

## 11. Pending-atlas-screen flag was sticky — Low

`OpenGuiWrapper.pendingAtlasMap` (added by the `fix desync` commit) was never cleared on disconnect,
so a set flag would hijack whatever screen the player had open once some later world finished
loading its datapacks.

**Fix:** 10-second timeout, cleared when there's no client player, and it no longer replaces a
screen the player has already opened.

## 12. Caged boss's rerolled affixes never applied — Medium

`ImprisonedMonsterBlock` calls `createMobRarityEdit(...)` (which runs `setupNewMobOnSpawn`, the stat
pass) and *then* `randomizeAffixes(bossRarity)`. `MobData.randomizeAffixes` only mutates the affix
id list — it does no recalculation. So the captive kept Boss-rarity stats with none of the affixes
it had just rolled.

Note the contrast: the uber/pinnacle handlers in `DungeonAddonEvents` do call
`recalcStats_DONT_CALL()` after `setRarity`.

**Fix:** added `Load.Unit(mob).recalcStats_DONT_CALL()` after the affix reroll.

---

# Round 2 — playtest bug reports

Four bugs reported from testing. **None of them were fixed by findings 1–12.** Root causes below.

## Attribution: which commit caused 13–15?

The report was that the talent tree was fine before ~July 16. It was worth checking properly, so
here is the full accounting. The window is `53295bc3..HEAD` — `53295bc3` (Jul 9, "gen data") is the
last commit before Jul 16; the next commit is Jul 18. So the window covers the entire Atlas push.

Everything in the talent tree render surface that changed in that window:

```
database/data/talent_tree/TalentTree.java              10 ++++    (new ATLAS enum constant)
gui/screens/skill_tree/AtlasPassiveTreeScreen.java     55 +++++   (new file, its own screen)
gui/screens/skill_tree/buttons/PerkButton.java          6 +--
```

And nothing else. No `SkillTreeScreen` change, no `VertexContainer` / `SkillTreeRenderType` /
`BufferInfo` change, no mixin change, no `Perk.java` change, no regenerated perk JSON, no change to
`talents.json` / `ascendancy.json`, no change to any of the 308 talent icons, and `RUN_DEV_TOOLS` was
already `false`. The Library-of-Exile bump in the window (`48853f01..6b7e0148`) touches map
generation, relics and one Gson null guard — nothing GUI.

**So no commit in the window introduced the lag.** Findings 13 and 14 are long-standing bugs
inherited from upstream. What actually explains the reported "talents and ascendancy broken, atlas
passive fine" split is scale, not code — the three screens run identical code over very different
amounts of data:

| tree | perk buttons rendered | distinct perk textures |
|------|----------------------|------------------------|
| talents | **961** | 121 |
| ascendancy | 212 | 116 |
| atlas passives | **10** | 10 |

At ~130 distinct textures the old code allocated ~130 `RenderType` objects **and forced ~130 buffer
flushes every frame**, plus hashing 2883 `BufferInfo` records (each containing a `Matrix4f`) into a
`HashMultimap`. At 10 textures none of that is noticeable. That is a ~96x difference in per-frame
work between the talents tree and the atlas passive tree, running the same code.

The one honest link to the date is finding 15b below — a real in-window change that plausibly made
the pre-existing ordering bug *visible* without causing it.

## 15b. `PerkButton`'s newbie-dimming branch changed which school it checks — Low, but see below

The 6-line `PerkButton` change in the window:

```java
- if (playerData.talents.getAllocatedPoints(TalentTree.SchoolType.TALENTS) < 1) {
-     opacity = this.perk.getType() == Perk.PerkType.START ? 1 : 0.2F;
+ if (playerData.talents.getAllocatedPoints(school.getSchool_type()) < 1) {
+     opacity = this.perk.is_entry ? 1 : 0.2F;
```

This is a correct fix in itself (the old code checked TALENTS points on *every* tree, so the
ascendancy screen stopped dimming as soon as you spent one talent point). But it changes what is
actually drawn: a player with 0 points in the school being viewed now gets **every** non-entry node
forced to `opacity = 0.2`, with its icon at `0.2 + 0.2 = 0.4`.

Combined with finding 14's arbitrary draw order, a 0.2-alpha colour quad landing on top of a
0.4-alpha icon is exactly the reported "the colour of the node is rendering on top, blocking the
icon". So this change very plausibly took a latent ordering bug and made it obvious on the two trees
where the player had no points allocated.

This is a hypothesis, not something proven — but it is the only in-window change that alters what
the tree draws, and it is left in place because the behaviour it implements is the intended one. It
is finding 14's fix that makes it render correctly.

## 13. Talent tree lag — a new `RenderType` allocated per texture per frame — High

`SkillTreeRenderType.getSkillTreeRenderType()` built a fresh `RenderType.CompositeState` +
`TextureStateShard` + `TransparencyStateShard` on **every call**, and `VertexContainer.draw()` calls
it once per distinct texture **every frame**.

The container is keyed by `ResourceLocation`, and every perk contributes its own icon texture plus a
status-dependent colour and border texture. On the talents tree that's several hundred distinct keys,
so several hundred `RenderType` objects were being allocated and thrown away every single frame.

It's worse than the allocation: a freshly built `RenderType` never compares equal to the previous
one, so `MultiBufferSource` could not batch. It flushed its buffer on **every group** — hundreds of
draw calls per frame where there should be a handful.

**Fix:** `SkillTreeRenderType` now caches one `RenderType` per texture in a `HashMap` and reuses it.

## 14. Tree draw order decided by hash iteration order — High

`VertexContainer` stored quads in a `HashMultimap<ResourceLocation, BufferInfo>` and drew them by
iterating `map.asMap()`. `HashMultimap` has no defined iteration order, so *which texture group
reached the screen last was arbitrary*.

The per-quad z (`BufferInfo.pBlitOffset`, set to -5/-3/-2/-1 for connection/colour/border/icon) does
not rescue this: the skill tree render type draws blended quads with no depth write, so within a
frame the last thing drawn wins regardless of z. Hence connection lines on top of nodes, and an
allocated node's colour covering its own icon.

`HashMultimap` was also a poor fit for two other reasons: it hashes every `BufferInfo` (including its
`Matrix4f`) on insert, and being set-backed it silently drops any two quads that compare equal.

**Fix:** `VertexContainer` now holds `TreeMap<layer, LinkedHashMap<texture, List<BufferInfo>>>` with
four explicit layers (`LAYER_CONNECTION` < `LAYER_PERK_COLOR` < `LAYER_PERK_BORDER` <
`LAYER_PERK_ICON`), and `draw()` calls `bufferSource.endBatch()` between layers — the flush is what
actually guarantees ordering. `PerkButton` and `SkillTreeScreen.renderConnection` pass a layer when
submitting. No value hashing, no dedupe, deterministic frame to frame.

## 15. Search bar drawn under the tree — Medium

`render()` called `renderPanels(gui)` (header bar, point counts, search box) and `tips.render(...)`
*before* the tree. Everything renders into the same GUI layer with no depth write, so the tree's
nodes and lines painted straight over the search box.

**Fix:** the header chrome now renders after the tree, once the zoom pose has been popped back to
1:1. `renderBackgroundDirt` stays where it was, behind everything.

## 16. Atlas node not credited on map completion, server only — High

`AtlasData.ensureInitialized()` latched its `initialized` flag before checking whether it had
anything to iterate:

```java
if (initialized) return;
initialized = true;                                  // latched unconditionally
for (AtlasNode node : DungeonDatabase.AtlasNodes().getList()) { ... }
```

`initialized` is `transient`, so on a dedicated server it starts `false` after the capability is
deserialized on login — and login can run before the atlas node registry is populated. When that
happened the method latched having unlocked **nothing**, permanently, for that session. The
`ON_MAP_FULLY_CLEARED` handler gates on `if (!pd.atlas.isUnlocked(node.id)) continue;`, so every node
was skipped and completing a map credited nothing.

Singleplayer never reproduced it because the integrated server already has the registry loaded before
any of this runs — matching the report exactly.

**Fix:** `ensureInitialized()` returns without latching when the node registry is empty, so it
retries on the next call.

> The `/kill` detail in the report is a red herring — it isn't how the mobs died that mattered, it's
> that the player's `unlockedNodes` was empty for the whole session.

## Bug 4 (missing atlas nodes on first join, fine after rejoin) — already fixed by finding 10

This is finding **10** above, and the reported symptom is the strongest evidence for it.

`AtlasNodeLayout` parsed its grid at registry order 20 while dungeon_realm's `ATLAS_NODE` loads at
order 52, and `AtlasGridPoint` classifies a cell as a node by asking
`AtlasNodes().isRegistered(id)`. On a **first** join to a dedicated server the node registry was not
yet populated when the layout packet arrived, so every cell came back "not a node" and the map
rendered with nodes missing. On a **rejoin** the registry was still populated from the first session,
so the re-parse succeeded — precisely the reported "exit and rejoin resolved it".

Moving the layout to order 53 makes the nodes always load first. No further change needed, but this
one is worth explicitly re-testing on a dedicated server with a fresh client.

---

## Verified clean during this audit

- Localization — every new `Chats` / `Words` / `Itemtips` entry is present in `en_us.json`.
- Block resources — blockstates and models exist for all six new blocks; they reference vanilla
  textures, and `Def.block` registers no `BlockItem`, so no item models are required.
- No duplicate GUIDs across the 45 new loot-stat classes.
- `IRarity.UNCOMMON` is the correct `"uncommon"` GUID (the odd one out among the `*_ID` constants).
- `AtlasData`'s `Set<String>` fields persist correctly — `LoadSave` is plain Gson.
- `BASE_SEEDS` / `PERFECTED_SEEDS` are non-empty `List.of` constants, so `dropPotentialSeed`'s
  `nextInt(pool.size())` can't throw.
- `AtlasGrid` is a faithful copy of the working `TalentGrid`, including its edge-bounds assumptions.
- `QuickUsePotionPacket`'s rewritten stream is correct (strongest usable potion per type).

## Known and accepted — not changed

- **Zoom vs. hover offset.** `AtlasMapScreen` scales the pose but passes raw `mouseX`/`mouseY` to
  `super.render`, so click/hover targets drift from the drawn positions when zoomed out.
  `SkillTreeScreen` has done the same thing for a long time; fixing it means fixing both together.
- **`RerollProphecyPacket` has no cooldown**, unlike `TeleportToBossPacket`'s 50-tick guard. Server
  side validates map, reroll cap, and coin cost, and `rerollsUsed` bounds the expensive path, so a
  spamming client only produces chat messages.
- **Strongbox `LEVER_CLICK` every 20 ticks** while guardians live — deliberate per its comment, but
  it is a permanent per-second sound for an encounter with no timer.
- **Shrine's duplicate-buff avoidance retries only once**, so "Twin Blessing" can still roll the
  same buff twice.

## Skill tree — remaining known costs (not addressed)

Findings 13–15 removed the per-frame allocation and fixed ordering, but the screen still does real
work per perk per frame that nothing caches:

- `PerkButton.render` → `TalentsData.getStatus` → `canAllocate` → `hasFreePoints` →
  `PlayerPointsType.getFreePoints(p)`, which re-reads config and re-derives the point total. Once per
  perk, every frame.
- `canAllocate` also calls `getAllAllocatedPerks(...)` for any perk with a non-empty `one_kind`
  (27 perks currently). That re-filters the entire `TalentTrees` registry and allocates a new list
  each time.

None of this changed in this push, and it should be far less noticeable now that the render path
isn't flushing hundreds of buffers a frame. If the tree still feels heavy after testing, cache the
per-frame status computation in `PerkScreenContext` (which is already rebuilt once per frame for
exactly this kind of thing) rather than optimising the callees.

## Follow-ups worth doing later

- The three encounter blocks (`StrongboxBlock`, `ImprisonedMonsterBlock`, `ShrineBlock`) now share
  most of their shape: activate → track remaining → resolve recipient → reward → consume. Worth a
  common base class; the divergence between strongbox and imprisoned-monster tracking is exactly
  what caused finding #3.
- The six new blocks all use placeholder models pointing at vanilla textures (see the `comment`
  fields in their model JSON). They need real art before release.
- `AtlasNodeLayout` assumes a single entry (`mainCalcData` returns the first). If multiple layouts
  ever become a thing, this needs a real lookup key.
