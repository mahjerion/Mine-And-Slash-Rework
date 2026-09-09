package com.robertx22.mine_and_slash.database.data.mercenary;

import com.robertx22.mine_and_slash.capability.entity.CooldownsData;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.spells.components.ComponentPart;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.ProjectileCastHelper;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.TargetSelector;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.CastingWeapon;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
    /**
     * The mercenary engages at this fraction of a skill's actual reach rather than on its exact edge.
     * A target that takes one step while the cast winds up would otherwise walk straight out of it.
     */
    private static final double ENGAGE_FACTOR = 0.8D;

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

        // the projectile speed multiplier is a property of the mercenary, so it is sampled at most
        // once per tick and shared by every skill scored on that tick - building a SpellCastContext
        // per skill would fire a full stat event four times over. see projSpeedMulti.
        double[] projMulti = {-1D};

        Spell walkTo = null;
        double walkToRange = 0;

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
            // a self heal at full health is a wasted cooldown. `continue` rather than `return` on
            // purpose - the skill is skipped and the slot behind it gets its turn on this same tick.
            if (heldForFullHealth(merc, spell)) {
                continue;
            }
            if (unit.getCooldowns().isOnCooldown(spell.GUID())) {
                continue;
            }

            // a skill only connects inside its own radius now that it is cast from the mercenary
            // rather than from the enemy. out of range, walk in first rather than burning the
            // cooldown on a swing at empty air.
            double engage = engageRange(merc, spell, projMulti);

            if (merc.distanceToSqr(target) <= engage * engage) {
                beginCast(merc, spell, target, engage);
                // the list order IS the priority queue, so the first castable skill wins and the
                // rest wait for the next opening.
                return;
            }

            // out of range. remembered rather than acted on straight away, so a skill further down
            // the list that CAN be cast from here still fires this tick instead of the mercenary
            // losing two seconds walking in for this one. the first one out of range is the one it
            // falls back to, which keeps list order deciding between the walks themselves.
            if (walkTo == null) {
                walkTo = spell;
                walkToRange = engage;
            }
        }

        if (walkTo != null) {
            cast.startApproach(walkTo, target, walkToRange, APPROACH_TIMEOUT_TICKS);
        }
    }

    /**
     * One shot of the ranged basic attack the mercenary's weapon grants it - a staff's Bolt.
     * <p>
     * Kept here rather than on the entity because this is the class that owns casting a spell from a
     * non player, and the body below is the same {@link #fire} primitive every other mercenary cast
     * goes through. It is paced entirely by its callers (the combat goals, on their existing ranged
     * interval), which is why there is no cooldown of any kind in here:
     * <ul>
     * <li>not {@code GLOBAL_COOLDOWN} - {@link #onTick} returns early while that is up, so charging a
     * basic attack to it would starve the mercenary's four real skills;</li>
     * <li>not {@code spell.GUID()} either, for the reason {@code ProcSpellEffect.procCooldownKey}
     * documents: a basic attack must not grey out or lock the same skill if it is also slotted.</li>
     * </ul>
     * No {@code isUsable} check: this skill is deliberately not on the mercenary class's grid. The
     * half of that gate which does apply - the casting weapon predicate - is checked in
     * {@code MercenaryEntity.weaponBasicAttackSpell()}, where the weapon is already in hand.
     */
    public static void castWeaponBasicAttack(MercenaryEntity merc, Spell spell, LivingEntity target) {
        try {
            SpellCastContext ctx = new SpellCastContext(merc, 0, spell);

            // swings even for a skill authored swing_arm: false, which Bolt is. That flag is set
            // because a player gets the spell's own Player Animator animation, which a mob has no
            // equivalent of - the same reason beginCast swings at all, and the arrow path too.
            merc.swing(InteractionHand.MAIN_HAND);

            fire(merc, spell, ctx, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * How far a weapon granted basic attack actually reaches, so a goal can hold fire rather than
     * lob one at something it cannot cross. Read off the skill's own components, the same way the
     * approach logic sizes a slotted skill - Bolt's 6 tick, 2.5 speed projectile answers ~12 blocks.
     */
    public static double basicAttackRange(MercenaryEntity merc, Spell spell) {
        return engageRange(merc, spell, new double[]{-1D});
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
                double engage = cast.approachEngageRange;
                cast.clearApproach();
                beginCast(merc, spell, target, engage);
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
     * How close the mercenary has to be for this skill to connect, in blocks.
     * <p>
     * Read off the skill's own components rather than declared as a new datapack field, so a pack
     * that retunes a radius or a projectile speed moves the mercenary's engagement distance with it
     * and cannot forget to.
     * <p>
     * {@code AoeSelector} multiplies its radius by the caster's Area stat at runtime, which this
     * estimate does not know about. That errs on the near side: the mercenary closes slightly more
     * than it strictly had to, which costs a step, where the other direction would cost a whiffed
     * cooldown.
     */
    private static double engageRange(MercenaryEntity merc, Spell spell, double[] projMulti) {

        Reach reach = reachOf(spell);

        if (reach.unlimited) {
            // nothing to close on - it lands on the target itself, or it is not aimed at an enemy
            // at all. cast it from wherever the mercenary happens to be standing.
            return merc.getAttributeValue(Attributes.FOLLOW_RANGE);
        }

        double range = reach.onCastRange;

        if (reach.projTravel > 0) {
            // a projectile reaches as far as it flies before it expires, plus whatever it does when
            // it gets there. this is the same speed times lifespan that ProjectileCastHelper uses to
            // size its own enemy search, and the same multiplier SummonProjectileAction applies to
            // shootSpeed - so a Projectile Speed mercenary really does engage from further out.
            range = Math.max(range, reach.projTravel * projSpeedMulti(merc, spell, projMulti) + reach.detonationRadius);
        } else {
            range = Math.max(range, reach.detonationRadius);
        }

        return Math.max(range * ENGAGE_FACTOR, MIN_CAST_RANGE);
    }

    /**
     * The mercenary's Projectile Speed, sampled at most once per tick.
     * <p>
     * A stat effect could in principle be conditioned on a spell tag and so differ between two of
     * the mercenary's skills, but this number only decides where the mercenary stands, never what
     * the projectile does, so one sample is close enough - and it saves firing a full
     * {@code SpellStatsCalculationEvent} once per equipped skill per tick.
     */
    private static double projSpeedMulti(MercenaryEntity merc, Spell spell, double[] cache) {
        if (cache[0] < 0) {
            try {
                SpellCastContext ctx = new SpellCastContext(merc, 0, spell);
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
     * target's own position, and a skill with nothing aimed at an enemy anywhere in it. The second
     * one matters more than it sounds - every self buff and ally heal used to fall through to the
     * {@code MIN_CAST_RANGE} floor, which walked a kiting caster into melee to buff itself.
     * <p>
     * {@code huntsEnemy} separates those two, because they are only the same question for movement.
     * {@link #heldForFullHealth} needs to know whether the skill is aimed at anything hostile at
     * all, and "lands on the target's own position" very much is.
     */
    private record Reach(boolean unlimited, boolean huntsEnemy, double onCastRange, double projTravel,
                         double detonationRadius) {
        /** aimed at an enemy, but with no distance to close - it lands on the target itself */
        static final Reach ON_TARGET = new Reach(true, true, 0, 0, 0);
        /** nothing in the skill is aimed at an enemy: a self buff, an ally heal */
        static final Reach HELPS_ONLY = new Reach(true, false, 0, 0, 0);
    }

    private static Reach reachOf(Spell spell) {
        Reach cached = RANGE_CACHE.get(spell);
        if (cached == null) {
            cached = computeReach(spell);
            RANGE_CACHE.put(spell, cached);
        }
        return cached;
    }

    /**
     * Computed once per spell - it is a function of datapack data that does not change while loaded.
     * <p>
     * Keyed on the Spell instance rather than its id, and weak, so a datapack reload invalidates it
     * for free: the reload builds new Spell objects, the old ones become unreachable, and their
     * entries go with them. An id-keyed cache would happily serve a pre-reload radius forever.
     * Server thread only, from the mercenary tick.
     */
    private static final Map<Spell, Reach> RANGE_CACHE = new WeakHashMap<>();

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
                    // real flight, after vanilla per-tick air drag - see ProjectileCastHelper
                    projTravel = Math.max(projTravel, ProjectileCastHelper.travelDistance(speed, life));
                }
            }

            for (MapHolder sel : part.targets) {
                // only what the skill is trying to hit sets how close it has to be. a self buff or
                // an ally heal uses the same aoe selector with an ally predicate, and gating those
                // on the enemy's distance would send a kiting caster into melee to buff itself.
                if (!huntsEnemies(sel)) {
                    continue;
                }
                // and only a selector that describes a DISTANCE marks the skill as range limited. a
                // self or target selector hits what it hits from wherever the caster is standing, so
                // it says nothing about how close the mercenary has to be. letting one of those set
                // the flag is what walked a self buff into melee: it marked the skill range limited
                // and then contributed no range, leaving it on the MIN_CAST_RANGE floor. a self
                // selector declares no en_predicate either, so huntsEnemies' "assume hostile when
                // there is no predicate" fallback lets it through in the first place.
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

        return new Reach(false, true, onCastRange, projTravel, detonation);
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

    /**
     * Whether this is a self-maintenance skill the mercenary has no reason to spend yet.
     * <p>
     * A mercenary used to drink its Hunter's Potion and sit down to Meditate the instant they came
     * off cooldown, at full health, and then have nothing left when it actually needed them.
     * <p>
     * Derived from the skill rather than declared as a new datapack field, so the modpack's own
     * mercenary spell set is covered without editing thirty files - and so a pack that writes a new
     * heal gets the behaviour for free. Held back when the skill is tagged {@code heal} AND nothing
     * in it hunts an enemy: that second half is what keeps a damage skill which happens to also heal
     * (the pack's Frost Nova) firing normally, and it reuses the reach analysis the approach logic
     * already computes and caches per spell.
     * <p>
     * The threshold is {@code ServerContainer.MERCENARY_HEAL_SKILL_HP_THRESHOLD}.
     */
    private static boolean heldForFullHealth(MercenaryEntity merc, Spell spell) {
        if (!spell.is(SpellTags.heal)) {
            return false;
        }
        if (reachOf(spell).huntsEnemy()) {
            // it is aimed at something hostile, so it is a damage skill that happens to also heal
            return false;
        }
        float max = merc.getMaxHealth();
        if (max <= 0) {
            return false;
        }
        return merc.getHealth() / max > ServerContainer.get().MERCENARY_HEAL_SKILL_HP_THRESHOLD.get();
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
     * Both cooldowns go on here, at the START of the cast: they are what stops a wind up the mercenary
     * had to abandon - its target died, its owner switched it to Idle - from being re-picked on the
     * very next tick and locking it into a cast it never finishes, and for an instant skill, which
     * fires and returns without ever entering the cast state, this is the only place they go on.
     * <p>
     * The recovery is armed a second time when the cast actually ends, in {@link #armRecovery} - see
     * the note there for why arming it only here silently skipped it on every long cast.
     */
    private static void beginCast(MercenaryEntity merc, Spell spell, LivingEntity target, double engageRange) {
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

            merc.getCastState().start(spell, target, castTime, engageRange);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The recovery between skills, armed when a cast actually ends.
     * <p>
     * The player path does this in {@code SpellCastingData.onSpellCastFinished} - recovery starts when
     * the cast ends, never when it began. The mercenary was arming the global cooldown only in
     * {@link #beginCast}, so a skill whose cast time outruns its recovery spent the whole of it still
     * winding up: merc_whirlwind is a 100 tick cast with a 20 tick recovery, so the gate reopened at
     * tick 20 and the next skill went off on the tick after the whirlwind finished. Skills that resolve
     * instantly were unaffected, which is why only the long ones looked broken.
     */
    private static void armRecovery(MercenaryEntity merc, Spell spell) {
        try {
            SpellCastContext ctx = new SpellCastContext(merc, 0, spell);
            Load.Unit(merc).getCooldowns().setOnCooldown(CooldownsData.GLOBAL_COOLDOWN, spell.getCastSpeedTicks(ctx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** one tick of a wind up: keep aiming, pace any repeats, fire when the timer runs out */
    private static void tickCast(MercenaryEntity merc, MercenaryCastState cast) {
        try {
            Spell spell = cast.spell;

            // only two things stop a cast now: the mercenary dying, and its owner switching it to
            // Idle. Losing the target used to end it too, which quietly threw away most of every
            // multicast - a 100 tick, ten pulse whirlwind that killed its target on pulse three
            // stopped there and the next skill went off instead. Nothing else can interrupt: onTick
            // returns above while a cast runs, so no skill coming off cooldown reaches this.
            if (spell == null || !merc.isAlive() || merc.getMode() == MercenaryData.CombatMode.IDLE) {
                if (spell != null) {
                    // an abandoned cast still owes the recovery, or stopping mid cast would be a
                    // free instant skill at whatever the mercenary turns on next
                    armRecovery(merc, spell);
                }
                cast.clear();
                return;
            }

            LivingEntity target = liveTarget(merc, cast);

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
                    SpellCastContext ctx = new SpellCastContext(merc, cast.ticksDone, spell);
                    ctx.castNumber = castsByThisTick;
                    ctx.castsTotal = timesToCast;
                    fire(merc, spell, ctx, target);
                }
            }

            if (cast.ticksLeft <= 0) {
                if (timesToCast <= 1) {
                    fire(merc, spell, new SpellCastContext(merc, cast.ticksDone, spell), target);
                }
                armRecovery(merc, spell);
                cast.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            cast.clear();
        }
    }

    /**
     * Who the running cast is aimed at right now, re-acquiring if the original died.
     * <p>
     * A multicast outlives its first victim by design - that is the whole point of the change in
     * {@link #tickCast} - so the pulses that are left need somewhere to go. Preference order is the
     * mercenary's own current target (whatever its target goals have moved on to), then the nearest
     * thing it is allowed to hit inside the skill's engage range.
     * <p>
     * Null is a legitimate answer: nothing hostile is left. The cast still runs to the end, because a
     * self centred area skill like whirlwind never needed a target to land, and the recovery is owed
     * either way. The result is deliberately not written back into {@code cast.target} - re-asking
     * each tick is what lets it pick up whatever wanders in next.
     */
    @Nullable
    private static LivingEntity liveTarget(MercenaryEntity merc, MercenaryCastState cast) {
        LivingEntity target = cast.target;

        if (target != null && target.isAlive() && !target.isRemoved()) {
            return target;
        }

        LivingEntity current = merc.getTarget();
        if (current != null && current.isAlive() && !current.isRemoved()) {
            return current;
        }

        LivingEntity owner = merc.getOwner();
        if (owner == null) {
            return null;
        }

        double range = Math.max(cast.engageRange, MIN_CAST_RANGE);
        LivingEntity nearest = null;
        double nearestSqr = range * range;

        for (LivingEntity other : merc.level().getEntitiesOfClass(
                LivingEntity.class, merc.getBoundingBox().inflate(range))) {

            // mayEngage rather than summonShouldAttack directly, so the fallback aim can't land a
            // skill on a player nobody is fighting
            if (other == merc || !other.isAlive() || !merc.mayEngage(owner, other)) {
                continue;
            }
            double distSqr = merc.distanceToSqr(other);
            if (distSqr <= nearestSqr) {
                nearestSqr = distSqr;
                nearest = other;
            }
        }
        return nearest;
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
    private static void fire(MercenaryEntity merc, Spell spell, SpellCastContext ctx, @Nullable LivingEntity target) {
        // aiming: point the mercenary at what it is fighting, so projectiles leave it on the right
        // heading instead of flying wherever it happened to be facing.
        aimAt(merc, target);

        SpellCtx c = SpellCtx.onCast(merc, ctx.calcData).setCastIndex(ctx.castNumber, ctx.castsTotal);
        c.target = target;

        // last, for the same reason ProcSpellEffect does it last
        spell.attached.onCast(c);
    }

    /**
     * Point the mercenary at what it is casting on.
     * <p>
     * Pitch matters as much as yaw and used not to be set: {@code ProjectileCastHelper} reads
     * {@code caster.getXRot()} in its constructor, and a mob's xRot is only ever moved by LookControl,
     * which eases towards a target over several ticks. So the first projectile after acquiring
     * something flew on a stale pitch, and one aimed up or down a slope never had the right one.
     * Setting it here fixes that for every mercenary projectile skill, not just the basic attack.
     * <p>
     * Null once a running multicast has outlived everything hostile near it - see liveTarget. There
     * is simply nothing to turn towards, and the mercenary keeps whatever heading it had.
     */
    private static void aimAt(MercenaryEntity merc, @Nullable LivingEntity target) {
        if (target == null) {
            return;
        }
        merc.getLookControl().setLookAt(target, 30F, 30F);
        merc.setYRot(yawTowards(merc, target));
        merc.setXRot(pitchTowards(merc, target));
        merc.yHeadRot = merc.getYRot();
        merc.yBodyRot = merc.getYRot();
        merc.xRotO = merc.getXRot();
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
}
