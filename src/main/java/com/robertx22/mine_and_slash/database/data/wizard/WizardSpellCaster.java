package com.robertx22.mine_and_slash.database.data.wizard;

import com.robertx22.mine_and_slash.database.data.spells.components.ComponentPart;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.TargetSelector;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.database.data.wizard.entity.WizardEntity;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.robertx22.library_of_exile.main.ExileLog;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.WeakHashMap;

/**
 * The skill AI of a wizard monster: pick something that reaches, wind it up, fire it.
 * <p>
 * A pared down {@code MercenarySpellCaster}. The range engine below is lifted from it wholesale
 * because it solves the same problem - a datapack skill declares no range, so the only honest
 * answer is to read one off its own components - but the selection around it is deliberately much
 * simpler, and different in three ways:
 * <ul>
 * <li><b>Random, not prioritised.</b> A mercenary walks a priority queue because its owner chose
 * and ordered its four skills. A wizard's list is flat and the pick is uniform, so the same mob
 * feels different from one fight to the next.</li>
 * <li><b>No cooldowns of any kind.</b> Not the skills' own - those are authored for a player who
 * ranks them and pays mana, and a three skill wizard honouring them would stand idle most of a
 * fight with no basic attack to fall back on - and no global cooldown either. The interval on
 * {@link WizardType} is the entire pacing mechanism.</li>
 * <li><b>No walk-in phase.</b> {@code WizardCombatGoal} already holds the wizard at its active
 * skill's distance, and a skill that doesn't reach is simply not picked this tick.</li>
 * </ul>
 */
public class WizardSpellCaster {

    /** a contact skill still needs contact, whatever its components say */
    private static final double MIN_CAST_RANGE = 2.5D;
    /**
     * The wizard engages at this fraction of a skill's actual reach rather than on its exact edge.
     * A target that takes one step while the cast winds up would otherwise walk straight out of it.
     */
    private static final double ENGAGE_FACTOR = 0.8D;
    /** how long a cast that lost its target sits before the next one, so a whiff isn't free */
    private static final int RETRY_TICKS = 10;

    public static void onTick(WizardEntity wizard) {

        if (wizard.level().isClientSide) {
            return;
        }

        WizardCastState cast = wizard.getCastState();

        // a dead or removed wizard drops whatever it was winding up. every spell entity it already
        // put in the world is independently safe with no caster - SimpleProjectileEntity removes
        // itself, StationaryFallingBlockEntity stops ticking damage and expires - so there is
        // nothing to clean up beyond forgetting the cast.
        if (!wizard.isAlive() || wizard.isRemoved()) {
            cast.clear();
            return;
        }

        // a cast in progress owns the wizard, the same way isCasting() owns the input on the player
        // path. nothing else is picked, and nothing fires, until it resolves.
        if (cast.isCasting()) {
            tickCast(wizard, cast);
            return;
        }

        LivingEntity target = wizard.getTarget();

        if (target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }

        if (wizard.nextCastTicks > 0) {
            wizard.nextCastTicks--;
            return;
        }

        WizardType type = wizard.getWizardType();
        if (type == null) {
            // A wizard has no melee at all (WizardEntity.doHurtTarget returns false), so no datapack
            // entry means no attack of any kind - it walks around and dies. At a quarter of every mob
            // list that is a large, silent difficulty drop plus free xp and loot, and the entity type
            // being registered is NOT evidence the entry exists: MobList.getRandomMob filters on the
            // entity type, so the two can diverge. Say so once per id rather than never.
            warnMissingType(wizard);
            return;
        }

        // the projectile speed multiplier is a property of the wizard, so it is sampled at most once
        // per tick and shared by every skill scored on that tick - building a SpellCastContext per
        // skill would fire a full stat event once each.
        double[] projMulti = {-1D};

        List<Spell> castable = new ArrayList<>();
        List<Double> ranges = new ArrayList<>();

        double distSqr = wizard.distanceToSqr(target);

        for (Spell spell : type.getSpells()) {
            double engage = engageRange(wizard, spell, projMulti);
            if (distSqr <= engage * engage) {
                castable.add(spell);
                ranges.add(engage);
            }
        }

        // nothing reaches from here. a short retry rather than the full interval, so a wizard that
        // has just walked into range opens fire within half a second instead of waiting out another
        // three seconds - but not every tick either: projSpeedMulti builds a SpellCastContext, and
        // that is a full stats calculation event. At a quarter of a map's monsters being wizards,
        // one per tick each while they close the distance is a real cost for an answer that barely
        // changes between ticks.
        if (castable.isEmpty()) {
            wizard.nextCastTicks = RETRY_TICKS;
            return;
        }

        // line of sight is checked once, on the skill actually chosen, rather than as a filter over
        // all of them: it is a raycast, and the answer is the same for every skill anyway.
        if (!wizard.getSensing().hasLineOfSight(target)) {
            wizard.nextCastTicks = RETRY_TICKS;
            return;
        }

        int i = wizard.getRandom().nextInt(castable.size());

        beginCast(wizard, castable.get(i), target, ranges.get(i));
        wizard.nextCastTicks = type.rollCastInterval(wizard.getRandom());
    }

    /**
     * Arms the skill and either fires it immediately (an instant) or starts its wind up.
     * <p>
     * No cooldown goes on here, unlike the mercenary version - see the class note. Nothing needs one:
     * a running cast blocks {@link #onTick} on its own, and the interval is rolled by the caller once
     * this returns, whether the skill fired instantly or is still winding up.
     */
    private static void beginCast(WizardEntity wizard, Spell spell, LivingEntity target, double engageRange) {
        try {
            SpellCastContext ctx = new SpellCastContext(wizard, 0, spell);

            // the player's own cast swing is commented out in Spell.cast - players get an animation
            // from the Player Animator instead, which a mob has no equivalent of. so the wizard
            // swings here, respecting the spell's own swing_arm flag, as the cast STARTS - a skill
            // with a wind up gets a tell instead of landing out of nowhere.
            if (spell.config.swing_arm) {
                wizard.swing(InteractionHand.MAIN_HAND);
            }

            aimAt(wizard, target);

            int castTime = castTimeTicksFor(spell, ctx);

            if (castTime <= 1) {
                fire(wizard, spell, ctx, target);
                return;
            }

            wizard.getCastState().start(spell, target, castTime, engageRange);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * How long this skill winds up for before it goes off.
     * <p>
     * A channel is deliberately treated as instant: its {@code cast_time_ticks} is the gap between
     * pulses rather than a wind up, and keeping one going needs an input held down, which a mob has
     * no equivalent of. Author a repeating wizard skill as {@code multiCast}, not {@code channel}.
     */
    private static int castTimeTicksFor(Spell spell, SpellCastContext ctx) {
        if (spell.getConfig().isChannel()) {
            return 0;
        }
        return spell.getCastTimeTicks(ctx);
    }

    /** one tick of a wind up: keep aiming, pace any repeats, fire when the timer runs out */
    private static void tickCast(WizardEntity wizard, WizardCastState cast) {
        try {
            Spell spell = cast.spell;

            if (spell == null) {
                cast.clear();
                return;
            }

            LivingEntity target = liveTarget(wizard, cast);

            cast.ticksLeft--;
            cast.ticksDone++;

            // keep tracking through the wind up, so a target that moved during it is still aimed at
            aimAt(wizard, target);

            int timesToCast = spell.getConfig().times_to_cast;

            if (timesToCast > 1) {
                // the pacing Spell.onCastingTick does, repeated here rather than called: the ctx it
                // hands to Spell.cast builds a bare SpellCtx with no target and no position source,
                // so every repeat would land wherever the wizard happens to face.
                int castsByLastTick = (cast.ticksDone - 1) * timesToCast / cast.totalTicks;
                int castsByThisTick = cast.ticksDone * timesToCast / cast.totalTicks;

                if (castsByThisTick != castsByLastTick) {
                    fire(wizard, spell, new SpellCastContext(wizard, cast.ticksDone, spell), target);
                }
            }

            if (cast.ticksLeft <= 0) {
                if (timesToCast <= 1) {
                    fire(wizard, spell, new SpellCastContext(wizard, cast.ticksDone, spell), target);
                }
                cast.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            cast.clear();
        }
    }

    /**
     * Who the running cast is aimed at right now, falling back if the original died.
     * <p>
     * A multicast can outlive its first victim, so the pulses that are left need somewhere to go.
     * Null is a legitimate answer - a self centred area skill never needed a target to land, and the
     * cast runs to the end either way. Deliberately not written back into {@code cast.target}, so
     * re-asking each tick picks up whatever the wizard's target goals move on to.
     */
    @Nullable
    private static LivingEntity liveTarget(WizardEntity wizard, WizardCastState cast) {
        LivingEntity target = cast.target;

        if (target != null && target.isAlive() && !target.isRemoved()) {
            return target;
        }

        LivingEntity current = wizard.getTarget();
        if (current != null && current.isAlive() && !current.isRemoved()) {
            return current;
        }

        return null;
    }

    /**
     * The cast itself - the point the skill's own components actually run.
     * <p>
     * The position source is left at the default {@code SOURCE_ENTITY}, which is the wizard. A skill
     * that has to land on the player asks for that explicitly, through {@code SUMMON_AT_SIGHT},
     * which drops onto the target's own position for a non player caster. Forcing the position to
     * the target here instead would move every component of every skill: projectiles would spawn
     * inside the player and fly onward, and a five block nova would connect from forty blocks away.
     */
    private static void fire(WizardEntity wizard, Spell spell, SpellCastContext ctx, @Nullable LivingEntity target) {
        aimAt(wizard, target);

        SpellCtx c = SpellCtx.onCast(wizard, ctx.calcData);
        c.target = target;

        spell.attached.onCast(c);
    }

    /**
     * Point the wizard at what it is casting on.
     * <p>
     * Pitch matters as much as yaw: {@code ProjectileCastHelper} reads {@code caster.getXRot()} in
     * its constructor, and a mob's xRot is only ever moved by LookControl, which eases towards a
     * target over several ticks. Without this the first projectile after acquiring something flies
     * on a stale pitch, and one aimed up or down a slope never has the right one.
     */
    private static void aimAt(WizardEntity wizard, @Nullable LivingEntity target) {
        if (target == null) {
            return;
        }
        wizard.getLookControl().setLookAt(target, 30F, 30F);
        wizard.setYRot(yawTowards(wizard, target));
        wizard.setXRot(pitchTowards(wizard, target));
        wizard.yHeadRot = wizard.getYRot();
        wizard.yBodyRot = wizard.getYRot();
        wizard.xRotO = wizard.getXRot();
    }

    private static float yawTowards(LivingEntity from, LivingEntity to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90F);
    }

    // eye to eye, so a projectile leaves at head height and arrives at head height rather than
    // aiming at the target's feet
    private static float pitchTowards(LivingEntity from, LivingEntity to) {
        double dx = to.getX() - from.getX();
        double dy = to.getEyeY() - from.getEyeY();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, horizontal));
    }

    // ------------------------------------------------------------------ how far a skill reaches

    /**
     * How close the wizard has to be for this skill to connect, in blocks.
     * <p>
     * Read off the skill's own components rather than declared as a datapack field, so a pack that
     * retunes a radius or a projectile speed moves the mob's engagement distance with it and cannot
     * forget to.
     * <p>
     * Clamped at Follow Range on the way out, which the mercenary version does not need: a
     * mercenary's skills were authored for it, while a wizard's are cut down from player skills that
     * can carry very long lived projectiles. A skill whose components imply eighty blocks would
     * otherwise be "in range" of anything the wizard can even see, and it would fire the moment it
     * acquired a target rather than when it had actually closed.
     */
    private static double engageRange(WizardEntity wizard, Spell spell, double[] projMulti) {

        double max = wizard.getAttributeValue(Attributes.FOLLOW_RANGE);

        Reach reach = reachOf(spell);

        if (reach.unlimited) {
            // nothing to close on - it lands on the target itself, or it is not aimed at an enemy at
            // all. cast it from wherever the wizard happens to be standing.
            return max;
        }

        double range = reach.onCastRange;

        if (reach.projTravel > 0) {
            // a projectile reaches as far as it flies before it expires, plus whatever it does when
            // it gets there. same speed times lifespan ProjectileCastHelper uses to size its own
            // enemy search, and the same multiplier SummonProjectileAction applies to shootSpeed.
            range = Math.max(range, reach.projTravel * projSpeedMulti(wizard, spell, projMulti) + reach.detonationRadius);
        } else {
            range = Math.max(range, reach.detonationRadius);
        }

        return Math.min(max, Math.max(range * ENGAGE_FACTOR, MIN_CAST_RANGE));
    }

    /**
     * The wizard's Projectile Speed, sampled at most once per tick.
     * <p>
     * This number only decides where the wizard stands, never what the projectile does, so one
     * sample shared across every skill scored on a tick is close enough - and it saves firing a full
     * stats calculation event once per skill per tick.
     */
    private static double projSpeedMulti(WizardEntity wizard, Spell spell, double[] cache) {
        if (cache[0] < 0) {
            try {
                SpellCastContext ctx = new SpellCastContext(wizard, 0, spell);
                cache[0] = ctx.calcData.data.getNumber(EventData.PROJECTILE_SPEED_MULTI, 1).number;
            } catch (Exception e) {
                cache[0] = 1;
            }
        }
        return cache[0];
    }

    /**
     * What a skill can reach, worked out once from its components.
     * <p>
     * {@code unlimited} covers the two cases with no distance to close: an action that lands on the
     * target's own position, and a skill with nothing aimed at an enemy anywhere in it.
     */
    private record Reach(boolean unlimited, double onCastRange, double projTravel, double detonationRadius) {
        /** aimed at an enemy, but with no distance to close - it lands on the target itself */
        static final Reach ON_TARGET = new Reach(true, 0, 0, 0);
        /** nothing in the skill is aimed at an enemy */
        static final Reach HELPS_ONLY = new Reach(true, 0, 0, 0);
    }

    /**
     * Computed once per spell - a function of datapack data that does not change while loaded.
     * <p>
     * Keyed on the Spell instance rather than its id, and weak, so a datapack reload invalidates it
     * for free: the reload builds new Spell objects, the old ones become unreachable, and their
     * entries go with them. An id-keyed cache would serve a pre-reload radius forever. Server thread
     * only, from the wizard tick.
     */
    private static final Map<Spell, Reach> RANGE_CACHE = new WeakHashMap<>();

    private static Reach reachOf(Spell spell) {
        Reach cached = RANGE_CACHE.get(spell);
        if (cached == null) {
            cached = computeReach(spell);
            RANGE_CACHE.put(spell, cached);
        }
        return cached;
    }

    private static Reach computeReach(Spell spell) {

        double onCastRange = 0;
        double projTravel = 0;
        boolean huntsAnything = false;

        for (ComponentPart part : spell.attached.on_cast) {

            for (MapHolder act : part.acts) {
                if (SpellAction.SUMMON_AT_SIGHT.GUID().equals(act.type)) {
                    // for anything that isn't a player this drops straight onto the target's own
                    // position - see SummonAtSightAction - so there is no distance to close
                    return Reach.ON_TARGET;
                }
                if (SpellAction.SUMMON_PROJECTILE.GUID().equals(act.type)) {
                    huntsAnything = true;
                    double life = act.getOrDefault(MapField.LIFESPAN_TICKS, 0D);
                    double speed = act.getOrDefault(MapField.PROJECTILE_SPEED, 0D);
                    projTravel = Math.max(projTravel, life * speed);
                }
            }

            for (MapHolder sel : part.targets) {
                // only what the skill is trying to hit sets how close it has to be, and only a
                // selector that describes a DISTANCE says anything about it - a self or target
                // selector hits what it hits from wherever the caster is standing.
                if (!huntsEnemies(sel)) {
                    continue;
                }
                if (TargetSelector.AOE.GUID().equals(sel.type)) {
                    huntsAnything = true;
                    onCastRange = Math.max(onCastRange, sel.getOrDefault(MapField.RADIUS, 0D));
                } else if (TargetSelector.IN_FRONT.GUID().equals(sel.type)) {
                    huntsAnything = true;
                    onCastRange = Math.max(onCastRange, sel.getOrDefault(MapField.DISTANCE, 0D));
                }
            }
        }

        // the widest enemy hunting area on whatever the skill leaves behind. an orb that travels six
        // blocks and detonates for four can hit something ten blocks away, and a field that never
        // moves reaches exactly its own radius.
        double detonation = 0;
        boolean entitiesHuntEnemies = false;

        for (List<ComponentPart> parts : spell.attached.entity_components.values()) {
            for (ComponentPart part : parts) {
                for (MapHolder sel : part.targets) {
                    if (!huntsEnemies(sel)) {
                        continue;
                    }
                    entitiesHuntEnemies = true;
                    if (TargetSelector.AOE.GUID().equals(sel.type)) {
                        detonation = Math.max(detonation, sel.getOrDefault(MapField.RADIUS, 0D));
                    }
                }
            }
        }

        if (!huntsAnything && !entitiesHuntEnemies) {
            return Reach.HELPS_ONLY;
        }

        return new Reach(false, onCastRange, projTravel, detonation);
    }

    /**
     * Whether this selector is looking for something to hit rather than something to help.
     * <p>
     * Written as a list of the ally predicates rather than of the hostile ones on purpose: a
     * predicate nobody thought of here counts as hostile, which costs the wizard a few steps it did
     * not need to take. Guessing the other way would put it back to casting at nothing.
     */
    private static boolean huntsEnemies(MapHolder selector) {
        try {
            AllyOrEnemy pred = selector.getEntityPredicate();
            return pred != AllyOrEnemy.allies
                    && pred != AllyOrEnemy.allies_not_self
                    && pred != AllyOrEnemy.pets
                    && pred != AllyOrEnemy.casters_summons;
        } catch (Exception e) {
            // no predicate declared at all - assume it is aimed at something
            return true;
        }
    }

    /**
     * Ids already reported, so a map full of inert wizards logs four lines and not four per tick.
     * <p>
     * Static and never cleared on purpose: the set holds at most one short string per wizard entity
     * type in the game, and a datapack reload that fixes the entry makes this stop being reached
     * anyway. Concurrent because entity ticks are not guaranteed to be one thread across dimensions.
     */
    private static final Set<String> WARNED_MISSING_TYPES = ConcurrentHashMap.newKeySet();

    private static void warnMissingType(WizardEntity wizard) {
        try {
            var key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(wizard.getType());
            String id = key == null ? "unknown" : key.getPath();
            if (WARNED_MISSING_TYPES.add(id)) {
                ExileLog.get().warn("Wizard '" + id + "' has no mmorpg_wizard datapack entry, so it cannot"
                        + " cast anything and has no melee attack either - it will be a harmless mob."
                        + " Regenerate the mmorpg datapack, or check whether a pack removed the entry.");
            }
        } catch (Exception e) {
            // a log line must never be the thing that breaks an entity tick
        }
    }
}
