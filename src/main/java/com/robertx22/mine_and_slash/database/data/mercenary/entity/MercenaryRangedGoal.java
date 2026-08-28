package com.robertx22.mine_and_slash.database.data.mercenary.entity;

import com.robertx22.mine_and_slash.database.data.mercenary.MercenarySpellCaster;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The combat goal a "ranged" {@link com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass.AiBehavior}
 * mercenary (e.g. the elementalist) uses instead of {@link net.minecraft.world.entity.ai.goal.MeleeAttackGoal}.
 * <p>
 * Keeps its distance from the target rather than closing in - the real damage for a caster comes from
 * {@link MercenarySpellCaster}, which runs independently of movement and casts regardless of range - and
 * only throws a melee hit when the target ends up inside melee reach anyway. That covers both a target
 * that corners the mercenary despite the kiting, and a ranged-behaviour mercenary with no actual ranged
 * weapon equipped, which otherwise would just stand at range doing nothing between spell casts.
 */
public class MercenaryRangedGoal extends Goal {

    private static final double MELEE_REACH = 2.0D;
    private static final double MELEE_REACH_SQR = MELEE_REACH * MELEE_REACH;

    // the band the mercenary tries to hold: flee once the target is inside it, close back in past it
    private static final double KITE_DISTANCE = 8.0D;
    private static final double KITE_DISTANCE_SQR = KITE_DISTANCE * KITE_DISTANCE;

    private static final int MELEE_ATTACK_INTERVAL = 20;
    private static final int RANGED_ATTACK_INTERVAL = 20;

    // how long a retreat path is allowed to run before a new one is asked for. every moveTo to a raw
    // position is a fresh A* over a region snapshot - the flee point slides along with the mercenary,
    // so PathNavigation's "same target as last time" cache almost never hits and re-pathing every tick
    // both costs real time and keeps resetting the path out from under the mercenary's feet.
    private static final int FLEE_REPATH_INTERVAL = 10;

    private final MercenaryEntity merc;
    private final double speedModifier;
    private int meleeCooldown;
    private int rangedCooldown;
    private int fleeRepathCooldown;

    public MercenaryRangedGoal(MercenaryEntity merc, double speedModifier) {
        this.merc = merc;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // without this the goal is only ticked on every second server tick (Mob.serverAiStep hands the
    // odd ones to tickRunningGoals(false), which skips any goal that hasn't opted in) - which silently
    // doubled every interval below and dropped movement updates to 10Hz. MeleeAttackGoal opts in too.
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = merc.getTarget();
        // a creative or spectator player can't be fought and must not be kited forever. canAttack()
        // isn't cover for this: it only gates targets NearestAttackableTargetGoal picks, and one
        // handed over by OwnerHurtTargetGoal never passes through it.
        return target != null && target.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        meleeCooldown = 0;
        rangedCooldown = 0;
        fleeRepathCooldown = 0;
        merc.setAggressive(true);
    }

    @Override
    public void stop() {
        // nothing else ever clears a creative/spectator target, so the mercenary would re-aggro the
        // moment it could. MeleeAttackGoal drops it here for the same reason.
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(merc.getTarget())) {
            merc.setTarget(null);
        }
        merc.setAggressive(false);
        merc.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = merc.getTarget();
        if (target == null) {
            return;
        }

        merc.getLookControl().setLookAt(target, 30F, 30F);
        double distSqr = merc.distanceToSqr(target);

        // MercenarySpellCaster owns the navigation while it walks a skill into range. without this
        // the fleeFrom below undoes every step on the tick after it is taken, and a kiting caster
        // can never close on anything - its short range skills would be unusable by design.
        if (!merc.isApproachingForCast()) {
            if (distSqr <= MELEE_REACH_SQR) {
                meleeAttack(target);
            } else if (distSqr < KITE_DISTANCE_SQR) {
                fleeFrom(target);
            } else {
                merc.getNavigation().moveTo(target, speedModifier);
            }
        } else if (distSqr <= MELEE_REACH_SQR) {
            // still swinging on the way in - the approach is not a truce
            meleeAttack(target);
        }

        // fires at any range with line of sight - performRangedAttack itself is a no-op when the
        // mercenary isn't actually holding a ranged weapon, which is exactly the fallback wanted for a
        // caster: keep kiting and let its spells do the work between melee opportunities.
        if (merc.getSensing().hasLineOfSight(target)) {
            tryRangedAttack(target);
        }

        if (meleeCooldown > 0) {
            meleeCooldown--;
        }
        if (rangedCooldown > 0) {
            rangedCooldown--;
        }
        if (fleeRepathCooldown > 0) {
            fleeRepathCooldown--;
        }
    }

    private void meleeAttack(LivingEntity target) {
        merc.getNavigation().stop();
        if (meleeCooldown > 0) {
            return;
        }
        // mid cast the mercenary keeps kiting but throws nothing. no cooldown is spent here either, so
        // the poke lands on the first tick after the cast instead of a second later
        if (merc.isCastingSpell()) {
            return;
        }
        merc.swing(InteractionHand.MAIN_HAND);
        merc.doHurtTarget(target);
        meleeCooldown = MELEE_ATTACK_INTERVAL;
    }

    private void fleeFrom(LivingEntity target) {
        // let the current retreat run until it finishes or the timer is up, the way vanilla's own
        // kiting goal (AvoidEntityGoal) does - it paths once and simply waits for isDone().
        if (fleeRepathCooldown > 0 && !merc.getNavigation().isDone()) {
            return;
        }
        // set before the checks below on purpose: a cornered mercenary with nowhere to go shouldn't
        // re-roll getPosAway (ten random attempts of its own) on every single tick.
        fleeRepathCooldown = FLEE_REPATH_INTERVAL;

        // unlike a raw "four blocks directly backwards" vector, this only ever returns somewhere the
        // mercenary can actually stand on and path to, so backing into terrain or off a ledge is out.
        Vec3 away = DefaultRandomPos.getPosAway(merc, (int) KITE_DISTANCE, 4, target.position());
        if (away == null) {
            return;
        }
        // and refuse a "retreat" that doesn't actually gain any ground
        if (target.distanceToSqr(away) < target.distanceToSqr(merc)) {
            return;
        }
        merc.getNavigation().moveTo(away.x, away.y, away.z, speedModifier);
    }

    private void tryRangedAttack(LivingEntity target) {
        if (rangedCooldown > 0) {
            return;
        }
        // performRangedAttack refuses mid cast anyway - the point of checking here as well is not to
        // burn the interval on a shot that never left the bow
        if (merc.isCastingSpell()) {
            return;
        }
        merc.performRangedAttack(target, 1.0F);
        rangedCooldown = RANGED_ATTACK_INTERVAL;
    }
}
