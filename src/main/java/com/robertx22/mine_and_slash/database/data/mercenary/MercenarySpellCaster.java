package com.robertx22.mine_and_slash.database.data.mercenary;

import com.robertx22.mine_and_slash.capability.entity.CooldownsData;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.spells.components.ComponentPart;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.TargetSelector;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.CastingWeapon;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Drives a mercenary's active skills.
 * <p>
 * Modelled on {@link com.robertx22.mine_and_slash.uncommon.effectdatas.rework.action.ProcSpellEffect},
 * which is the one path in the codebase that casts a spell from an arbitrary LivingEntity with no
 * player assumptions anywhere in it. The player's own state machine ({@code SpellCastingData}) is not
 * reusable here - it lives on PlayerData, reads key inputs, and gates almost everything on
 * {@code instanceof Player}.
 * <p>
 * Skills cost nothing: the design gives mercenaries no mana or energy, so {@code spendResources} is
 * never called and no resource check gates a cast.
 * <p>
 * Cast time is honoured: a skill authored with {@code cast_time_ticks} spends that long winding up in
 * {@link MercenaryCastState} before it goes off, instead of resolving on the tick it was picked. The
 * mercenary keeps walking and kiting through the wind up - only its attacks stop, see
 * {@link MercenaryEntity#isCastingSpell()}.
 */
public class MercenarySpellCaster {

    /** how long the mercenary will chase a skill's range before giving up on that skill */
    private static final int APPROACH_TIMEOUT_TICKS = 20 * 2;
    /** how long a skill it failed to reach sits out, so it stops winning the priority queue */
    private static final int APPROACH_FAIL_COOLDOWN_TICKS = 20 * 3;
    private static final double APPROACH_SPEED = 1.15D;
    /** repath this often rather than every tick - moveTo is the expensive call in this loop */
    private static final int APPROACH_REPATH_INTERVAL = 5;
    /** a contact skill still needs contact, whatever its components say */
    private static final double MIN_CAST_RANGE = 2.5D;

    public static void onTick(MercenaryEntity merc) {

        MercenaryCastState cast = merc.getCastState();

        // a cast in progress owns the mercenary, the same way isCasting() owns the input on the player
        // path. no other skill is picked, and nothing fires, until this one resolves.
        if (cast.isCasting()) {
            tickCast(merc, cast);
            return;
        }

        // so does the walk in before one - the skill is already chosen, it just isn't close enough yet
        if (cast.isApproaching()) {
            tickApproach(merc, cast);
            return;
        }

        LivingEntity target = merc.getTarget();

        // "Skills can only be cast while the Mercenary is actively in combat" - no target, no casting.
        if (target == null || !target.isAlive() || !merc.isAlive()) {
            return;
        }
        if (merc.getMode() == MercenaryData.CombatMode.IDLE) {
            return;
        }

        MercenaryData data = merc.getMercData();
        if (data == null) {
            return;
        }

        EntityData unit = Load.Unit(merc);

        // one skill at a time, same as a player - without this every off cooldown skill would fire on
        // the same tick the moment a target appeared.
        if (unit.getCooldowns().isOnCooldown(CooldownsData.GLOBAL_COOLDOWN)) {
            return;
        }

        for (int i = 0; i < MercenaryClass.EQUIPPED_SKILLS; i++) {
            Spell spell = data.getEquippedSpell(i);
            if (spell == null) {
                continue;
            }
            // a skill the mercenary has slotted but has since dropped below the level for, or one it
            // is not holding the right weapon for
            if (!isUsable(merc, data, spell)) {
                continue;
            }
            if (unit.getCooldowns().isOnCooldown(spell.GUID())) {
                continue;
            }

            // a skill only connects inside its own radius now that it is cast from the mercenary
            // rather than from the enemy. out of range, walk in first rather than burning the
            // cooldown on a swing at empty air.
            double rangeSqr = castRangeSqr(merc, spell);

            if (merc.distanceToSqr(target) > rangeSqr) {
                cast.startApproach(spell, target, rangeSqr, APPROACH_TIMEOUT_TICKS);
                return;
            }

            beginCast(merc, spell, target);
            // the list order IS the priority queue, so the first castable skill wins and the rest
            // wait for the next opening.
            return;
        }
    }

    /**
     * One tick of walking into range of an already chosen skill.
     * <p>
     * Aborts on the same conditions {@link #tickCast} does, plus the skill going on cooldown or
     * becoming unusable underneath it - a weapon swap mid approach, most obviously. On timeout the
     * skill is put on a short cooldown instead of simply released: without that it wins the
     * priority queue again on the very next tick and the mercenary re-enters the same approach
     * forever, and every skill behind it in the list never gets a turn.
     */
    private static void tickApproach(MercenaryEntity merc, MercenaryCastState cast) {
        try {
            Spell spell = cast.approachSpell;
            LivingEntity target = cast.approachTarget;

            if (spell == null || target == null || !target.isAlive() || target.isRemoved()
                    || !merc.isAlive() || merc.getMode() == MercenaryData.CombatMode.IDLE) {
                cast.clearApproach();
                return;
            }

            // and it stops chasing the moment it is fighting something else - without this it walks
            // to whatever it was aiming at when the approach started while a new attacker hits it
            if (merc.getTarget() != target) {
                cast.clearApproach();
                return;
            }

            MercenaryData data = merc.getMercData();
            EntityData unit = Load.Unit(merc);

            if (data == null || !isUsable(merc, data, spell) || unit.getCooldowns().isOnCooldown(spell.GUID())) {
                cast.clearApproach();
                return;
            }

            aimAt(merc, target);

            if (merc.distanceToSqr(target) <= cast.approachRangeSqr) {
                cast.clearApproach();
                beginCast(merc, spell, target);
                return;
            }

            cast.approachTicksLeft--;

            if (cast.approachTicksLeft <= 0) {
                cast.clearApproach();
                unit.getCooldowns().setOnCooldown(spell.GUID(), APPROACH_FAIL_COOLDOWN_TICKS);
                return;
            }

            if (cast.approachTicksLeft % APPROACH_REPATH_INTERVAL == 0) {
                merc.getNavigation().moveTo(target, APPROACH_SPEED);
            }

        } catch (Exception e) {
            e.printStackTrace();
            cast.clearApproach();
        }
    }

    /**
     * How far this skill actually reaches, squared.
     * <p>
     * Read off the skill's own components rather than declared as a new datapack field, so a pack
     * that retunes a radius moves the mercenary's engagement distance with it and cannot forget to.
     * A skill that throws something - a projectile, or a meteor summoned at the target - is treated
     * as reaching as far as the mercenary can see; everything else is bounded by the widest area
     * its selectors search.
     * <p>
     * {@code AoeSelector} multiplies its radius by the caster's Area stat at runtime, which this
     * estimate does not know about. That errs on the near side: the mercenary closes slightly more
     * than it strictly had to, which costs a step, where the other direction would cost a whiffed
     * cooldown.
     */
    private static double castRangeSqr(MercenaryEntity merc, Spell spell) {

        Double cached = RANGE_CACHE.get(spell);

        if (cached == null) {
            cached = computeCastRange(spell);
            RANGE_CACHE.put(spell, cached);
        }

        double range = cached;

        if (range == RANGED_MARKER) {
            range = merc.getAttributeValue(Attributes.FOLLOW_RANGE);
        }
        return range * range;
    }

    /**
     * Computed once per spell - it is a function of datapack data that does not change while loaded.
     * <p>
     * Keyed on the Spell instance rather than its id, and weak, so a datapack reload invalidates it
     * for free: the reload builds new Spell objects, the old ones become unreachable, and their
     * entries go with them. An id-keyed cache would happily serve a pre-reload radius forever.
     * Server thread only, from the mercenary tick.
     */
    private static final Map<Spell, Double> RANGE_CACHE = new WeakHashMap<>();
    /** stand-in for "as far as it can see", resolved per mercenary from its follow range */
    private static final double RANGED_MARKER = -1D;

    private static double computeCastRange(Spell spell) {

        double range = MIN_CAST_RANGE;

        for (ComponentPart part : spell.attached.on_cast) {

            for (MapHolder act : part.acts) {
                // both of these leave the mercenary and travel, so the skill is not range limited
                if (SpellAction.SUMMON_PROJECTILE.GUID().equals(act.type)
                        || SpellAction.SUMMON_AT_SIGHT.GUID().equals(act.type)) {
                    return RANGED_MARKER;
                }
            }

            for (MapHolder sel : part.targets) {
                // only what the skill is trying to hit sets how close it has to be. a self buff or
                // an ally heal uses the same aoe selector with an ally predicate, and gating those
                // on the enemy's distance would send a kiting caster into melee to buff itself.
                if (!huntsEnemies(sel)) {
                    continue;
                }
                if (TargetSelector.AOE.GUID().equals(sel.type)) {
                    range = Math.max(range, sel.getOrDefault(MapField.RADIUS, MIN_CAST_RANGE));
                } else if (TargetSelector.IN_FRONT.GUID().equals(sel.type)) {
                    range = Math.max(range, sel.getOrDefault(MapField.DISTANCE, MIN_CAST_RANGE));
                }
            }
        }
        return range;
    }

    /**
     * Whether this selector is looking for something to hit rather than something to help.
     * <p>
     * Written as a list of the ally predicates rather than of the hostile ones on purpose: a
     * predicate nobody thought of here counts as hostile, which costs the mercenary a few steps it
     * did not need to take. Guessing the other way would put it back to casting at nothing.
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

    private static boolean isUsable(MercenaryEntity merc, MercenaryData data, Spell spell) {
        MercenaryClass mc = data.getMercClass();
        if (mc == null || !mc.isSkillUnlocked(spell.GUID(), data.lvl)) {
            return false;
        }
        return hasCastingWeapon(merc, spell);
    }

    /**
     * The weapon gate a player passes through in {@code SpellCastingData.canCast}. That check lives on
     * PlayerData, so it never ran here - a mercenary's skills advertised "Requires Mage Weapon" in the
     * mercenary screen (the tooltip comes from {@code Spell.GetTooltipString}) while it cast them bare
     * handed. Nothing about the predicates is player specific: they are typed to LivingEntity and read
     * {@code equipmentCache.getWeapon()}, which a mercenary has once MercenaryManager.applyGear has
     * mirrored its inventory onto the entity.
     * <p>
     * Only the declared {@code castingWeapon} predicate, deliberately not the player path's extra
     * "must be holding an MnS weapon at all" rule - {@link CastingWeapon#ANY_WEAPON} has to stay
     * castable by a freshly hired mercenary, or one would stand there doing nothing until geared.
     */
    private static boolean hasCastingWeapon(MercenaryEntity merc, Spell spell) {
        if (CompatConfig.get().ignoreWeaponReqForSpells()) {
            return true;
        }
        CastingWeapon req = spell.getConfig().castingWeapon;

        if (req.predicate.predicate.test(merc)) {
            return true;
        }
        // a battlemage casts magic off a melee weapon - the same exception the player path makes
        return req == CastingWeapon.MAGE_WEAPON && Load.Unit(merc).getUnit().isBattlemage();
    }

    /**
     * How long the skill itself is locked out for after a cast.
     * <p>
     * A charge skill has no cooldown of its own on purpose - {@code setChargesAndRegen} zeroes it
     * because the charge pool is meant to be the limiter - but that pool is {@code ChargeData} on
     * PlayerData, which a mercenary has none of. With nothing standing in for it the skill came back
     * on its bare recovery: merc_meteor is authored as 3 charges on a 20s regen and was firing every
     * 1.5 seconds. Pacing it by the regen time instead gives the rate the datapack asked for. It is a
     * steady one-per-regen rather than a real three-cast burst - that would need ChargeData decoupled
     * from Player and a sync path of its own.
     */
    private static int cooldownTicksFor(Spell spell, SpellCastContext ctx) {
        int cd = spell.getEffectiveCooldownTicks(ctx);
        if (spell.config.charges > 0) {
            return Math.max(cd, spell.getChargeCooldownTicks(ctx));
        }
        return cd;
    }

    /**
     * How long this skill winds up for before it goes off.
     * <p>
     * A channel is deliberately treated as instant here: its {@code cast_time_ticks} is the gap
     * between pulses rather than a wind up, and keeping one going needs an input held down, which a
     * mercenary has no equivalent of. It fires once and pays its recovery like every other skill.
     */
    private static int castTimeTicksFor(Spell spell, SpellCastContext ctx) {
        if (spell.getConfig().isChannel()) {
            return 0;
        }
        return spell.getCastTimeTicks(ctx);
    }

    /**
     * Arms the skill and either fires it immediately (an instant) or starts its wind up.
     * <p>
     * Both cooldowns go on here, at the START of the cast, never at the end. The player path arms them
     * in {@code onSpellCastFinished} instead, but here they are also what stops a wind up the
     * mercenary had to abandon - its target died, its owner switched it to Idle - from being re-picked
     * on the very next tick and locking it into a cast it never finishes.
     */
    private static void beginCast(MercenaryEntity merc, Spell spell, LivingEntity target) {
        try {
            SpellCastContext ctx = new SpellCastContext(merc, 0, spell);
            EntityData unit = Load.Unit(merc);

            unit.getCooldowns().setOnCooldown(spell.GUID(), cooldownTicksFor(spell, ctx));
            unit.getCooldowns().setOnCooldown(CooldownsData.GLOBAL_COOLDOWN, spell.getCastSpeedTicks(ctx));

            // the player's own cast swing is commented out in Spell.cast - players get a proper
            // animation from the Player Animator instead, which a mob has no equivalent of. so the
            // mercenary swings here, respecting the spell's own swing_arm flag. it swings as the cast
            // STARTS, so a skill with a wind up has a tell instead of landing out of nowhere.
            if (spell.config.swing_arm) {
                merc.swing(InteractionHand.MAIN_HAND);
            }

            aimAt(merc, target);

            int castTime = castTimeTicksFor(spell, ctx);

            if (castTime <= 1) {
                fire(merc, spell, ctx, target);
                return;
            }

            merc.getCastState().start(spell, target, castTime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** one tick of a wind up: keep aiming, pace any repeats, fire when the timer runs out */
    private static void tickCast(MercenaryEntity merc, MercenaryCastState cast) {
        try {
            Spell spell = cast.spell;
            LivingEntity target = cast.target;

            // a wind up is not a commitment: it dies with the thing it was aimed at, and an owner
            // flipping to Idle has to stop the mercenary mid cast, not one cast later
            if (spell == null || target == null || !target.isAlive() || target.isRemoved()
                    || !merc.isAlive() || merc.getMode() == MercenaryData.CombatMode.IDLE) {
                cast.clear();
                return;
            }

            cast.ticksLeft--;
            cast.ticksDone++;

            // keep tracking through the wind up, so a target that moved during it is still aimed at
            aimAt(merc, target);

            int timesToCast = spell.getConfig().times_to_cast;

            if (timesToCast > 1) {
                // the pacing {@code Spell.onCastingTick} does, repeated here rather than called: the
                // ctx it hands to Spell.cast builds a bare SpellCtx with no target and no position
                // source, so every repeat would land wherever the mercenary happens to face.
                int castsByLastTick = (cast.ticksDone - 1) * timesToCast / cast.totalTicks;
                int castsByThisTick = cast.ticksDone * timesToCast / cast.totalTicks;

                if (castsByThisTick != castsByLastTick) {
                    fire(merc, spell, new SpellCastContext(merc, cast.ticksDone, spell), target);
                }
            }

            if (cast.ticksLeft <= 0) {
                if (timesToCast <= 1) {
                    fire(merc, spell, new SpellCastContext(merc, cast.ticksDone, spell), target);
                }
                cast.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            cast.clear();
        }
    }

    /**
     * The cast itself - the point the skill's own components actually run.
     * <p>
     * The position source is deliberately left at the default {@code SOURCE_ENTITY}, which for
     * {@code SpellCtx.onCast} is the mercenary. It used to be forced to {@code TARGET} so that
     * merc_meteor would land on the enemy, but that is what {@code ctx.getPos()} answers for every
     * component of every skill: {@code SummonProjectileAction} spawned the fireball inside the mob
     * and threw it onward, and {@code ComponentPart} handed the enemy's position to every target
     * selector, so a five block Frost Nova connected from forty blocks away. Meteor asks for the
     * enemy explicitly instead, via {@code MapField.POS_SOURCE} in {@code MercenarySpells} - the
     * route {@code SummonAtSightAction} already supports and merc_mage_circle already used.
     * <p>
     * {@code c.target} still points at what the mercenary is fighting, so targeted selectors and
     * homing projectiles lock on the same as before.
     */
    private static void fire(MercenaryEntity merc, Spell spell, SpellCastContext ctx, LivingEntity target) {
        // aiming: point the mercenary at what it is fighting, so projectiles leave it on the right
        // heading instead of flying wherever it happened to be facing.
        aimAt(merc, target);

        SpellCtx c = SpellCtx.onCast(merc, ctx.calcData);
        c.target = target;

        // last, for the same reason ProcSpellEffect does it last
        spell.attached.onCast(c);
    }

    private static void aimAt(MercenaryEntity merc, LivingEntity target) {
        merc.getLookControl().setLookAt(target, 30F, 30F);
        merc.setYRot(yawTowards(merc, target));
        merc.yHeadRot = merc.getYRot();
        merc.yBodyRot = merc.getYRot();
    }

    private static float yawTowards(LivingEntity from, LivingEntity to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90F);
    }
}
