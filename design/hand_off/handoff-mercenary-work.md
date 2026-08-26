# Hand-off: Mercenary GUI bug fixes + new AI-behavior feature

Repo: `C:\Users\Kelvin\Documents\GitHub\Mine-And-Slash-Rework` (Forge 1.20.1 mod, mod id `mmorpg`).
Branch: `1.20-Forge-mahj`. All touched files are part of an **uncommitted, in-progress** mercenary feature
(git status shows them as untracked `??`, not `M`) — there is no prior commit to diff against.

This session did two things back to back: (1) fixed four reported bugs in the Mercenary GUI, (2) added
a new datapack field so a mercenary class can declare melee vs. ranged AI. **Nothing in this session was
tested in-game** — no Minecraft client was launched from this tool session, so everything below is
verified only by reading code and a clean `compileJava`. Please treat in-game verification as the primary
thing to check, not just re-deriving correctness from the diffs.

---

## Part 1 — four bug fixes

### Bug 1: `MercClassArrowButton` showed a tooltip it shouldn't
**File:** `src/main/java/com/robertx22/mine_and_slash/gui/screens/mercenary/MercClassArrowButton.java`
Constructor unconditionally called `setTooltip(Tooltip.create(...))`; `AbstractButton` auto-renders
whatever tooltip is set on hover, and this button was never meant to show one. Removed the `setTooltip`
call, the now-dead `createTooltipPositioner()` override, and the imports that only existed for those two
things (`Tooltip`, `Words`, `TextUTIL`, `ChatFormatting`, `ClientTooltipPositioner`,
`DefaultTooltipPositioner`).
**Risk to double check:** low — purely subtractive, nothing else in the file referenced the removed code.

### Bug 2: `MercInfoButton` kept showing "not summoned" after the mercenary respawned
**File:** `src/main/java/com/robertx22/mine_and_slash/gui/screens/mercenary/MercInfoButton.java`
`renderWidget()` only flipped `tooltipDirty` when `screen.getMercLevel()` changed. `MercenaryScreen.rebuild()`
recreates this button and syncs the level *before* the entity has actually respawned, so the level the new
button captures already matches — it never changes again once the entity later spawns, so the cached
"not summoned" tooltip built right after rebuild never refreshed.
**Fix:** added a second dirty-tracking field:
```java
private boolean lastSummoned;
...
boolean summoned = screen.isMercSummoned();
if (summoned != lastSummoned) {
    lastSummoned = summoned;
    tooltipDirty = true;
}
```
placed next to the existing level check in `renderWidget()`.
**Risk to double check:** low. Worth eyeballing that `screen.isMercSummoned()` (defined in
`MercenaryScreen.java`, backed by `ClientMercenary.get() != null`) is cheap enough to call every frame —
it already was being called elsewhere in the same render path, so this doesn't add a new cost class.

### Bug 3: entering a map left the old mercenary alive in the overworld (two mercenaries on return)
**File:** `src/main/java/com/robertx22/mine_and_slash/database/data/mercenary/MercenaryManager.java`
**Root cause:** `getMerc(Player player)` looked the mercenary up via `player.level().getEntity(uuid)`.
Each `ServerLevel` (dimension) has its own independent entity manager, so the moment the player changes
dimension this can never find the entity again — it just silently returns `null`. That broke everything
downstream: `onPlayerTick`'s cross-dimension check (`merc.level() != player.level()`) was dead code (by
the time the dimension actually differs, `getMerc` already can't see it), `dismiss()` couldn't find the
entity to `.discard()` it, and `spawn()`'s safety-net dismiss call had the same blind spot before
overwriting `mercs.spawnedId` — permanently losing the only reference to the orphaned entity.

**Before:**
```java
public static MercenaryEntity getMerc(Player player) {
    if (player == null || player.level().isClientSide) {
        return null;
    }
    MercenaryStorageData mercs = Load.player(player).mercs;
    if (mercs.spawnedId == null) {
        return null;
    }
    if (!(player.level() instanceof ServerLevel level)) {
        return null;
    }
    Entity en = level.getEntity(mercs.spawnedId);
    if (en instanceof MercenaryEntity merc && merc.isAlive()) {
        return merc;
    }
    return null;
}
```

**After:**
```java
public static MercenaryEntity getMerc(Player player) {
    if (player == null || player.level().isClientSide) {
        return null;
    }
    MercenaryStorageData mercs = Load.player(player).mercs;
    if (mercs.spawnedId == null) {
        return null;
    }
    MinecraftServer server = player.level().getServer();
    if (server == null) {
        return null;
    }
    for (ServerLevel level : server.getAllLevels()) {
        Entity en = level.getEntity(mercs.spawnedId);
        if (en instanceof MercenaryEntity merc && merc.isAlive()) {
            return merc;
        }
    }
    return null;
}
```
Every caller (`dismiss`, `spawn`, `onPlayerTick`, `refreshGear`, `giveExp`) routes through `getMerc`, so
this one-method change is meant to make the existing cross-dimension dismiss/respawn logic in
`onPlayerTick` start working with no other code changes.

**Risk to double check — this is the fix I'd most want a second opinion on:**
- `Level#getServer()` / `MinecraftServer#getAllLevels()` — I confirmed the `player.getServer()` /
  `entity.getServer()` / `world.getServer()` pattern already exists elsewhere in this codebase (grep hits
  in `OnLogin.java`, `AllyOrEnemy.java`, `SpellUtils.java`), and the build compiles clean, but I have not
  run this in a real multi-dimension scenario (overworld → map → overworld) to confirm the entity is
  actually found and discarded. Iterating every loaded level once per `getMerc()` call is also a new cost
  — `getMerc` is called from `onPlayerTick` (every tick per player) and other hot paths; with only a
  handful of dimensions loaded at once this should be cheap, but worth a sanity check if the server ever
  has many concurrent map instances loaded (the addon `dungeon_realm`/other map leagues create per-instance
  dimensions — see `design/submodule-architecture.md`).
- Confirm `dismiss()` (line ~207) and `spawn()`'s safety-net call (line ~228) actually behave correctly
  now that `getMerc` can return an entity from a *different* dimension than the caller's `player.level()`
  — e.g. does `merc.discard()` work correctly when called while the player is no longer in that entity's
  level? (I believe `Entity.discard()` doesn't care which level the *caller* is in, only the entity's own
  level, so this should be fine, but it's exactly the kind of assumption worth re-checking.)

### Bug 4: Support/Aura gem sockets showed an empty item picker
**File:** `src/main/java/com/robertx22/mine_and_slash/vanilla_mc/packets/mercenary/MercenarySlotType.java`

**Aura half — confirmed and fixed.** `AURA.mayPlace()`'s spirit check called
`MercenaryManager.getMerc(owner)`, which is hard-coded to return `null` for a client-side player
(`if (player == null || player.level().isClientSide) return null;`). Since the picker (`GuiInventoryGrids
.ofMercSlotChoices`) is built client-side off the local player, this always fell back to the mercenary's
flat per-level spirit value instead of the real calculated (gear/aura-boosted) stat, which could
under/over-estimate remaining spirit relative to what the server would actually allow.

**Before:**
```java
int cost = (int) (gem.getAura().reservation * 100F);
return MercenaryStatUtils.getRemainingSpirit(MercenaryManager.getMerc(owner), data) >= cost;
```
**After:**
```java
int cost = (int) (gem.getAura().reservation * 100F);
MercenaryEntity merc = owner.level().isClientSide ? ClientMercenary.get() : MercenaryManager.getMerc(owner);
return MercenaryStatUtils.getRemainingSpirit(merc, data) >= cost;
```
`ClientMercenary.get()` (`database/data/mercenary/ClientMercenary.java`) is the mod's existing client-side
"find the local player's mercenary" accessor — `MercenaryScreen` already uses it the same way. This is a
straightforward side-aware dispatch, low risk.

**Support half — NOT actually fixed by code; turned out to be a stale build.** Two rounds of static
analysis (one dedicated Explore agent, one 45-tool-call deep dive) traced `SUPPORT.mayPlace()` end to end
and could not find a defect — every check, evaluated against the user's own confirmed facts (gem loose in
the main inventory, required level at/below the mercenary's level), should already have passed. I added a
temporary `ExileLog.get().warn(...)` diagnostic line inside `SUPPORT.mayPlace()` and asked the user to do
a clean rebuild + cold `runClient` restart before relying on it (the whole feature is uncommitted/new code,
so a stale hot-swapped class was suspected). **The user reported back that after this, both Support and
Aura gems started working**, so I removed the diagnostic line again. **No source change was made for the
Support path** — the current `SUPPORT.mayPlace()` body is byte-for-byte what it was before this session.
**Worth flagging to Opus:** since the Aura fix (a real code change) and the "just needed a clean rebuild"
explanation both landed at the same time, I cannot 100% rule out that the Aura code fix was actually
load-bearing for what the user perceived as "both now work" — i.e. it's possible the clean rebuild alone
would not have fixed Aura, and the user's single combined "both are working now" report doesn't
distinguish the two. If Opus wants to be thorough, worth asking the user to confirm Support specifically
still works on its own (it should, since nothing there changed, but double-confirming costs nothing).

---

## Part 2 — new feature: per-class mercenary AI behavior (melee vs. ranged)

**User's ask:** add a datapack field so a mercenary class can declare `melee` (current behavior: closes
distance, swings) or `ranged` (stays back and kites; only melees if the enemy gets close; if it actually
has a ranged basic attack, uses that normally while kiting). Fighter → melee (unchanged), Elementalist →
ranged (new).

### New datapack field
**File:** `src/main/java/com/robertx22/mine_and_slash/database/data/mercenary/MercenaryClass.java`
```java
/** how the mercenary moves and fights: melee closes in, ranged keeps its distance and kites */
public enum AiBehavior {
    MELEE, RANGED
}

public AiBehavior ai_behavior = AiBehavior.MELEE;
```
Added right after the existing `texture` field. `MercenaryClass` is a plain-field Gson-serialized
`JsonExileRegistry`/`IAutoGson` class with no prior enum-typed field of its own, so there's no established
in-file precedent for exactly this pattern. I based the "a plain Java enum field just works with this
codebase's Gson-backed (de)serialization" assumption on `SkillGemData.type` (`public SkillGemType type =
SkillGemType.SKILL;` in `saveclasses/skill_gem/SkillGemData.java`), which is a structurally identical
pattern (simple enum field, default value, no custom adapter) that is confirmed working in production
(that's the whole `SkillGemType.SUPPORT`/`AURA` mechanism from Part 1, bug 4). **I did not independently
verify this specific field round-trips through `MercenaryClass`'s own serializer** — `MercenaryClass`
implements `IAutoGson<MercenaryClass>` with `getClassForSerialization()` returning `MercenaryClass.class`,
which should mean it goes through the same generic Gson path, but this is inferred by analogy, not tested.
**No datapack JSON has been generated in this workspace at all** (`src/generated/resources` has no
mercenary output currently) — this field can't be confirmed end-to-end until someone does the
`RUN_DEV_TOOLS=true` + `runClient` login regen pass described in `CLAUDE.md`.

### Authoring: Elementalist opts into ranged
**File:** `src/main/java/com/robertx22/mine_and_slash/aoe_data/database/mercenaries/Mercenaries.java`
Added one line after `elementalist.texture = ...`:
```java
elementalist.ai_behavior = MercenaryClass.AiBehavior.RANGED;
```
Fighter was left untouched since `MELEE` is already the field's default (matches this file's existing
convention of only setting fields that differ from default).

### New Goal class: the actual kiting/fallback-melee AI
**File (new):** `src/main/java/com/robertx22/mine_and_slash/database/data/mercenary/entity/MercenaryRangedGoal.java`

There was **no existing kiting/ranged-AI pattern anywhere in this codebase** to reuse (grepped for
`kite`/`backAway`/custom `Goal` subclasses — this is the first one). The only prior art was vanilla
`RangedBowAttackGoal` (already used, unconditionally, alongside `MeleeAttackGoal`, in both
`MercenaryEntity` and its sibling `SummonEntity`) — but vanilla's fields are private, so it can't be
subclassed and partially overridden to add a melee-fallback; I wrote a small purpose-built `Goal` instead,
modeled on vanilla's general algorithm (kite band, flee/approach, attack-at-any-range-with-LOS) but with
its own state:

```java
public class MercenaryRangedGoal extends Goal {

    private static final double MELEE_REACH = 2.0D;
    private static final double MELEE_REACH_SQR = MELEE_REACH * MELEE_REACH;
    private static final double KITE_DISTANCE = 8.0D;
    private static final double KITE_DISTANCE_SQR = KITE_DISTANCE * KITE_DISTANCE;
    private static final int MELEE_ATTACK_INTERVAL = 20;
    private static final int RANGED_ATTACK_INTERVAL = 20;

    private final MercenaryEntity merc;
    private final double speedModifier;
    private int meleeCooldown;
    private int rangedCooldown;

    // canUse()/canContinueToUse(): target != null && target.isAlive()

    // tick():
    //   distSqr = merc.distanceToSqr(target)
    //   if distSqr <= MELEE_REACH_SQR      -> stop navigation, swing + doHurtTarget on a 20-tick cooldown
    //   else if distSqr < KITE_DISTANCE_SQR -> flee directly away from the target (moveTo a point 4
    //                                          blocks past the mercenary, opposite the target)
    //   else                                -> navigation.moveTo(target, speedModifier)   // close the gap
    //   independently of the above: if line-of-sight, try performRangedAttack on a 20-tick cooldown
    //   (this already no-ops in MercenaryEntity unless holding a bow/crossbow - see below)
}
```
(Full file has the complete implementation; this is the shape of it for review purposes.)

**Key design point:** `performRangedAttack(target, distanceFactor)` in `MercenaryEntity` already existed
and already no-ops unless `holdsRangedWeapon()` (main-hand is `BowItem`/`CrossbowItem`). I did **not**
change that method. So calling it unconditionally every tick (on cooldown, with LOS) from the new goal
means:
- A ranged-behavior mercenary *holding a bow* actually fires (via the existing `AutoAimingProj` homing
  projectile path) while kiting.
- A ranged-behavior mercenary *not* holding a bow (e.g. the Elementalist, who has no ranged weapon and
  deals damage entirely through `MercenarySpellCaster`-driven spells like Fireball/Meteor) just kites and
  falls back to melee if cornered — its real damage comes from `MercenarySpellCaster.onTick()`
  (`database/data/mercenary/MercenarySpellCaster.java`), which is called from `OnEntityTick.java`
  independently of any movement goal and has no range check of its own (it only checks `merc.getTarget()
  != null && target.isAlive()` and cooldowns) — so it keeps firing regardless of what the movement goal is
  doing. I verified this by reading `MercenarySpellCaster.onTick()` directly; it's unrelated to the goal
  system entirely.

**Risk to double check — the tuning constants are unvalidated guesses, not measured:**
`MELEE_REACH = 2.0`, `KITE_DISTANCE = 8.0`, both cooldowns `= 20` ticks. These numbers were picked by
analogy to the sibling `SummonEntity`'s `RANGED_ATTACK_RADIUS = 10F` / vanilla `RangedBowAttackGoal`
defaults, not tuned against actual mercenary movement speed or mob hitboxes in a running game. Expect
these to need adjustment after actually watching an Elementalist fight something.

**Risk to double check — the flee vector math:** `fleeFrom()` computes a point 4 blocks past the
mercenary directly away from the target and calls `navigation.moveTo(x, y, z, speed)`. This doesn't check
for obstacles/pathability of that exact point — vanilla's `PathNavigation.moveTo(double,double,double,...)`
should handle pathfinding toward it reasonably, but there's no fallback if that specific point is
unreachable (e.g. mercenary backed into a corner) — in that case the mercenary would presumably just fail
to move and eventually the target gets within `MELEE_REACH` and it switches to melee anyway, which is
probably fine, but wasn't tested.

### Wiring into `MercenaryEntity`
**File:** `src/main/java/com/robertx22/mine_and_slash/database/data/mercenary/entity/MercenaryEntity.java`

**Problem this had to solve:** `registerGoals()` is called once from the entity's constructor, but the
mercenary's class id (and therefore its `ai_behavior`) isn't set yet at that point — `setClassId(...)` is
called *after* construction, by `MercenaryManager.spawn()`. So the right combat goal can't be picked
inside `registerGoals()` itself.

**Before:** both goals registered unconditionally, same priority, for every mercenary:
```java
@Override
protected void registerGoals() {
    this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
    this.goalSelector.addGoal(5, new RangedBowAttackGoal<>(this, 1.0D, RANGED_ATTACK_COOLDOWN, RANGED_ATTACK_RADIUS));
    ... (unchanged goals below)
}
```
**After:**
```java
private Goal combatGoal;

@Override
protected void registerGoals() {
    refreshCombatGoal();
    ... (unchanged goals below, same as before)
}

private void refreshCombatGoal() {
    if (this.combatGoal != null) {
        this.goalSelector.removeGoal(this.combatGoal);
    }
    MercenaryClass mc = getMercClass();
    this.combatGoal = (mc != null && mc.ai_behavior == MercenaryClass.AiBehavior.RANGED)
            ? new MercenaryRangedGoal(this, 1.0D)
            : new MeleeAttackGoal(this, 1.0D, true);
    this.goalSelector.addGoal(5, this.combatGoal);
}
```
and `setClassId()` was changed from:
```java
public void setClassId(String id) {
    this.entityData.set(CLASS_ID, id == null ? "" : id);
}
```
to:
```java
public void setClassId(String id) {
    this.entityData.set(CLASS_ID, id == null ? "" : id);
    refreshCombatGoal();
}
```
So: at construction, `registerGoals()` runs with an empty class id → `getMercClass()` returns `null` →
defaults to `MeleeAttackGoal` (harmless placeholder). Then `MercenaryManager.spawn()` calls
`merc.setClassId(classId)` right after creating the entity, which re-evaluates and swaps in the correct
goal for the real class.

Also removed as no longer needed in this file: the `RangedBowAttackGoal` import and construction (fully
replaced by `MercenaryRangedGoal` for the ranged case), and the now-unused `RANGED_ATTACK_COOLDOWN` /
`RANGED_ATTACK_RADIUS` constants that existed solely to construct the old `RangedBowAttackGoal`.
`holdsRangedWeapon()` / `performRangedAttack()` were **not** touched — still exactly as before.

**Risk to double check — this is the piece I'd most want a second opinion on:**
1. **Behavior change for melee-class mercenaries that hold a bow.** Previously *every* mercenary
   (including Fighter) had both `MeleeAttackGoal` and `RangedBowAttackGoal` registered at the same
   priority. I reasoned (from general knowledge of vanilla's `GoalSelector` tie-breaking — goals of equal
   priority don't preempt each other once one is running, and insertion order likely gave
   `MeleeAttackGoal` first crack every tick since it was added first) that this was already effectively
   dead code for any mercenary not holding a bow, and probably *also* dead even when holding one, since
   melee likely always won the tie. **I did not verify this empirically in this session** — I only
   reasoned about it from how vanilla's goal selector is documented/generally known to behave. If that
   reasoning is wrong and `RangedBowAttackGoal` was in fact doing something useful before for a
   bow-wielding Fighter, this change is a regression: a melee-behavior mercenary now has *only*
   `MeleeAttackGoal`, full stop, even if a bow is somehow equipped in its main hand (gear slot legality
   doesn't currently forbid equipping a bow as a mercenary's mainhand weapon — `MercenarySlotType.GEAR
   .fitsSlot()` only checks `type.isWeapon()` generically). This is very likely an edge case nobody was
   relying on, but it's a behavior change worth Opus explicitly flagging as intentional/acceptable rather
   than assuming.
2. **`goalSelector.removeGoal(Goal)` semantics** — confirmed this method exists (compiles clean against
   the real `GoalSelector` class), but its runtime behavior when called on a goal that's currently
   *actively running* (e.g. if `setClassId` were ever called while `MeleeAttackGoal` was mid-swing) wasn't
   tested. In practice `setClassId` is only called once, immediately after the entity is created and
   before it's added to the world (`MercenaryManager.spawn()` calls `setClassId` before
   `finalizeSpawn`/`addFreshEntity`), so the placeholder `MeleeAttackGoal` should never actually be
   "running" when it's removed — but worth Opus double-checking that call order in `MercenaryManager
   .spawn()` hasn't changed since I read it, since this fix's correctness depends on it.
3. **`readAdditionalSaveData()` also calls `setClassId()`.** The class-level doc comment on
   `MercenaryEntity` says the entity is deliberately never saved (`shouldBeSaved()` returns `false`), which
   should make `addAdditionalSaveData`/`readAdditionalSaveData` dead code in practice — but I didn't trace
   every path that could call `readAdditionalSaveData` on a live entity (e.g. some copy/clone operation)
   to be 100% sure `refreshCombatGoal()` never fires at a surprising time from that path.

---

## Files touched this session

Bug fixes:
- `src/main/java/com/robertx22/mine_and_slash/gui/screens/mercenary/MercClassArrowButton.java`
- `src/main/java/com/robertx22/mine_and_slash/gui/screens/mercenary/MercInfoButton.java`
- `src/main/java/com/robertx22/mine_and_slash/database/data/mercenary/MercenaryManager.java`
- `src/main/java/com/robertx22/mine_and_slash/vanilla_mc/packets/mercenary/MercenarySlotType.java`

New feature:
- `src/main/java/com/robertx22/mine_and_slash/database/data/mercenary/MercenaryClass.java`
- `src/main/java/com/robertx22/mine_and_slash/aoe_data/database/mercenaries/Mercenaries.java`
- `src/main/java/com/robertx22/mine_and_slash/database/data/mercenary/entity/MercenaryRangedGoal.java` (new file)
- `src/main/java/com/robertx22/mine_and_slash/database/data/mercenary/entity/MercenaryEntity.java`

All four bug-fix files plus all four feature files are part of the same still-uncommitted mercenary
feature branch work (nothing has been committed by me this session).

## Verified so far
- `./gradlew compileJava` succeeds clean (only pre-existing, unrelated Curios-deprecation warnings).
- User confirmed, after a clean rebuild, that Support and Aura gem sockets now show legal items (see
  caveat above about whether this actually exercised the Aura code fix specifically).

## Not verified — recommended for Opus/user to check
1. In-game: map entry → overworld return no longer duplicates the mercenary (bug 3 — never tested live).
2. In-game: Offence/Defence tooltip correctly flips from "not summoned" to live stats after a class
   switch or level-up respawn (bug 2 — never tested live).
3. In-game: Fighter still fights exactly as before (no behavior regression from the `registerGoals()`
   restructure).
4. In-game: Elementalist actually kites, casts its spells from range, and falls back to melee when
   cornered — the entire new feature is currently unexercised outside of a successful compile.
5. Datapack regen: run `RUN_DEV_TOOLS=true` + `runClient`, log in, confirm the generated Elementalist JSON
   contains `"ai_behavior": "RANGED"` (or however the enum serializes) and that reloading it back into a
   fresh run doesn't trip `JsonExileRegistry.compareLoadedJsonAndFinalClass`'s stale-JSON check.
