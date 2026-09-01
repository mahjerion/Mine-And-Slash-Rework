package com.robertx22.mine_and_slash.database.data.wizard.entity;

import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryStrafe;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * How a wizard moves in a fight: hold a band, shuffle inside it, back off when crowded.
 * <p>
 * Adapted from {@code MercenaryRangedGoal}, minus its melee poke and its weapon granted basic
 * attack - a wizard has neither, its skills are all of its damage and
 * {@link com.robertx22.mine_and_slash.database.data.wizard.WizardSpellCaster} fires them
 * independently of anything here.
 * <p>
 * The band, rather than a single threshold, is what makes this read as a skeleton shuffling around
 * rather than a witch fleeing: with one distance to hold, a mob crosses it and reverses every tick.
 * Below {@code hold * FLEE_BAND} it retreats, above {@code hold} it closes, and in between it sways
 * in place. It is also why the wizard doesn't kite forever - the band has a near edge, so a player
 * who closes the distance gets a mob that stops running and stands its ground at melee range.
 */
public class WizardCombatGoal extends Goal {

    /** never so close that a skill authored for contact range can't be aimed */
    private static final double MIN_HOLD_DISTANCE = 2.0D;
    /** the distance held when no skill in progress says otherwise */
    private static final double KITE_DISTANCE = 9.0D;
    /** flee below hold * this, close back in past hold, do nothing in between */
    private static final double FLEE_BAND = 0.75D;
    /**
     * How long a retreat path is allowed to run before a new one is asked for. Every moveTo to a raw
     * position is a fresh A* over a region snapshot, and the flee point slides along with the wizard,
     * so PathNavigation's "same target as last time" cache almost never hits.
     */
    private static final int FLEE_REPATH_INTERVAL = 10;

    private final WizardEntity wizard;
    private final double speedModifier;
    private final MercenaryStrafe strafe = new MercenaryStrafe();
    private int fleeRepathCooldown;

    public WizardCombatGoal(WizardEntity wizard, double speedModifier) {
        this.wizard = wizard;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // without this the goal is only ticked on every second server tick - Mob.serverAiStep hands the
    // odd ones to tickRunningGoals(false), which skips any goal that hasn't opted in - which halves
    // the movement update rate and silently doubles every interval below.
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = wizard.getTarget();
        // a creative or spectator player can't be fought and must not be kited forever
        return target != null && target.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        fleeRepathCooldown = 0;
        wizard.setAggressive(true);
    }

    @Override
    public void stop() {
        // nothing else clears a creative/spectator target, so the wizard would re-aggro immediately
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(wizard.getTarget())) {
            wizard.setTarget(null);
        }
        wizard.setAggressive(false);
        wizard.getNavigation().stop();
        strafe.reset();
    }

    @Override
    public void tick() {
        LivingEntity target = wizard.getTarget();
        if (target == null) {
            return;
        }

        wizard.getLookControl().setLookAt(target, 30F, 30F);

        double distSqr = wizard.distanceToSqr(target);
        double hold = holdDistance();
        double holdSqr = hold * hold;

        if (distSqr < holdSqr * FLEE_BAND * FLEE_BAND) {
            strafe.reset();
            fleeFrom(target, hold);
        } else if (distSqr > holdSqr) {
            strafe.reset();
            wizard.getNavigation().moveTo(target, speedModifier);
        } else if (!wizard.isCastingSpell()) {
            // parked where it wants to be, waiting on its cast interval. sway rather than stand
            // rigid - the band above already owns the distance, so this is purely lateral.
            strafe.tick(wizard, target, 0F);
        }

        if (fleeRepathCooldown > 0) {
            fleeRepathCooldown--;
        }
    }

    /**
     * How far the wizard wants to stand from its target right now.
     * <p>
     * A skill being cast sets this to its own reach, so a five block skill is held at five blocks
     * rather than at the default kite distance - otherwise the wizard closes in for a short ranged
     * skill and flees back out of it before the cast finishes. Clamped at both ends: never inside
     * contact range, never further out than it would kite anyway.
     */
    private double holdDistance() {
        double active = wizard.getActiveEngageRange();
        if (active <= 0) {
            return KITE_DISTANCE;
        }
        return Mth.clamp(active, MIN_HOLD_DISTANCE, KITE_DISTANCE);
    }

    private void fleeFrom(LivingEntity target, double hold) {
        // let the current retreat run until it finishes or the timer is up, the way vanilla's own
        // kiting goal (AvoidEntityGoal) does - it paths once and simply waits for isDone().
        if (fleeRepathCooldown > 0 && !wizard.getNavigation().isDone()) {
            return;
        }
        // set before the checks below on purpose: a cornered wizard with nowhere to go shouldn't
        // re-roll getPosAway (ten random attempts of its own) on every single tick.
        fleeRepathCooldown = FLEE_REPATH_INTERVAL;

        // unlike a raw "four blocks directly backwards" vector, this only returns somewhere the
        // wizard can actually stand on and path to, so backing into terrain or off a ledge is out.
        Vec3 away = DefaultRandomPos.getPosAway(wizard, Math.max(2, (int) hold), 4, target.position());
        if (away == null) {
            return;
        }
        // and refuse a "retreat" that doesn't actually gain any ground
        if (target.distanceToSqr(away) < target.distanceToSqr(wizard)) {
            return;
        }
        wizard.getNavigation().moveTo(away.x, away.y, away.z, speedModifier);
    }
}
