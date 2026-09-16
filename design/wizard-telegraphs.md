# Wizard spell telegraphs — implementation notes

Wizard monsters (`fire_wizard`, `ice_wizard`, `lightning_wizard`, `chaos_wizard`) announce every skill
before it goes off:

1. A **telegraph phase** (`WizardSpellCaster.TELEGRAPH_TICKS`, 20 ticks) runs before every cast,
   instants included. While it runs, the skill's icon floats over the wizard's head and fills bottom to top.
2. A **red particle telegraph** shows where the skill lands. Area skills get a filled disc, under the
   target for an at-sight skill or around the wizard for a nova. Projectiles get a fan of lines, one per
   projectile.
3. At-sight skills (meteor, chilling field, lightning totem) land on the spot the target stood on
   **when the telegraph started**, not where they stand when it fires. That lets a player walk out of
   the circle.

Only `WizardEntity` does any of this. Players, mercenaries, summons and mob-rarity spells are unchanged.

## Server: `WizardCastState` has two phases

`startTelegraph(...)` sets `telegraphTicksLeft`. `tickCast` burns it down, then either fires (cast
time ≤ 1) or calls `beginCastPhase()`, which starts the old cast timer. The two counters are kept apart
on purpose: the `times_to_cast` pacing divides by `totalTicks`, so a multicast's repeats must never
count telegraph ticks.

`isCasting()` is true through both phases. That keeps `WizardCombatGoal` holding the skill's engage
range and not strafing during the telegraph.

`nextCastTicks` only counts down while no cast is running, so each cast adds the telegraph on top of
the rolled interval (+1s per cast). To take that second back out, subtract `TELEGRAPH_TICKS` where
the interval is rolled.

## Shapes: `WizardSpellShapes`

This is the component scan that used to be `WizardSpellCaster.computeReach`. One walk answers both
the engage-range question and the telegraph question:

- `SUMMON_AT_SIGHT` on cast → `AT_TARGET_CIRCLE`. The radius is the widest enemy aoe on the entities
  the skill leaves behind. The old code returned early here, before that scan.
- a projectile → `PROJECTILE_LINE`, sized by `ProjectileCastHelper.travelDistance`, plus
  `proj_count`/`proj_apart` for the fan.
- `IN_FRONT` → a single line of its distance.
- an enemy aoe on cast → `SELF_CIRCLE`.

This runs on the server thread only. The server scales the result by the wizard's stats (`AREA_MULTI`,
`PROJECTILE_SPEED_MULTI`, `BONUS_PROJECTILES`) and syncs the finished numbers. The client never reads
spell components.

## Sync: `SynchedEntityData` on `WizardEntity`

`publishCast(...)` runs once at telegraph start and `endCast()` runs once at the end. Nothing is sent
per tick. Progress is synced as a **start game time** (`CAST_START`), so a player who starts tracking
the wizard mid-cast still sees the correct fill. `CAST_SPELL` is set last and cleared to `""`. Every
client reader gates on it.

**Every path that ends a cast must call `endCast()`**, never `getCastState().clear()` directly.
Otherwise clients keep drawing a full icon and telegraph forever.

`CAST_FILL` vs `CAST_TOTAL`: an ordinary skill's icon fills to the moment it fires. A multicast's first
repeat lands partway into its cast time, so it fills over the telegraph only, then stays lit and
pulses until `CAST_TOTAL`.

## Client

- `WizardTelegraphRenderer` is hooked from `NeatRenderMixin` next to the health plate. It uses its own
  gates rather than the plate's, which hides past 12 blocks by default: 32 blocks, F1,
  invisibility, and `ClientConfigs.RENDER_WIZARD_TELEGRAPHS`. It draws the spell icon split at the fill line into two
  non-overlapping quads, dark above and lit below (overlapping them z-fought). It skips spells with
  no icon.
- Projectile lines are a camera-facing translucent red beam per projectile, drawn every frame in
  `WizardTelegraphRenderer.renderBeams` from the interpolated head rotation. Particles laid down in
  waves were gappy and left stale lines behind a turning wizard. The beam fades in with the fill and
  pulses through a multicast. It uses `NeatRenderType.getTelegraphBeamType()`. While a beam is up,
  `WizardEntity.getBoundingBoxForCulling()` grows by the beam length so an off-screen wizard's beam
  still renders, and the Neat plate focus raycast uses the real bounding box for wizards.
- `WizardTelegraphParticles` runs from `WizardEntity.tick()` on the client. It uses red
  `DustParticleOptions` (which `ParticlesPacket` can't carry), one wave every 4 ticks, and
  `sqrt`-distributed disc points so the fill is even. Each point snaps to a floor within 4 blocks down
  or 2 up, cached per column, and never reads unloaded chunks. The class has no client-only imports.
  Circles draw only during the telegraph. Projectile lines are not particles (see the beam above).

## Locked landing spot

`SpellCtx.lockedPos` is null everywhere except `WizardSpellCaster.fire`. `SummonAtSightAction` uses it
only for a non-player caster with no explicit `pos_source`.

## Icons

Spell icons resolve by GUID, so wizard spells need their own `witch_*.png` files, as the `merc_*.png`
files do for mercenaries. The mod ships ten copies of the player icons. Craft to Exile 2 reskins
icons under `config/openloader/resources/.../spells/icons/` and adds pack-only wizard spells. Its folder
also holds `witch_fire_nova`, `witch_lightning_totem` and `witch_putrid_breath` (copies of the pack's
own art), plus `witch_poison_cloud`. **A new wizard spell needs a matching `witch_<id>.png`**,
otherwise it just gets no icon.

## Gotchas

- Wizards never target creative or spectator players (`WizardCombatGoal.canUse`), so test in survival.
- Putrid Breath (pack) has a `channel` *tag* but `channel_skill: false`, so it is a multicast. If a
  pack sets `channel_skill: true`, `castTimeTicksFor` treats it as instant and it fires once.
- Dust lifetime is random (~8–40 ticks), so a disc can outlive the impact by a moment. The fix, if
  needed, is a short-lived custom particle type.
