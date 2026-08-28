package com.robertx22.mine_and_slash.database.data.mercenary.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Vanilla's {@link MeleeAttackGoal}, minus the swing while the mercenary is mid spell cast, plus a
 * small shuffle so a mercenary in contact with its target isn't a statue.
 * <p>
 * The goal is only overridden at the attack, never at {@code canUse}: it also owns the pathing that
 * closes the mercenary in on its target, and taking the whole goal away for the length of a wind up
 * would both stop it dead and hand the MOVE flag to RandomStrollGoal, which would walk it off to
 * wander mid fight. Blocking {@code doHurtTarget} on the entity instead isn't enough either - this is
 * the method that swings the arm, so that route leaves the mercenary miming hits that do nothing.
 */
public class MercenaryMeleeAttackGoal extends MeleeAttackGoal {

    /**
     * How much further than vanilla a mercenary reaches, in blocks.
     * <p>
     * Squared distances are not additive, so {@link #getAttackReachSqr} converts out of squared space
     * and back rather than adding to the squared value.
     */
    private static final double BONUS_REACH = 1.0D;

    /**
     * How close the mercenary wants to be, as a fraction of its reach, squared - so about 0.7 of it.
     * <p>
     * Once inside this it stops navigating and holds, which is the whole point: vanilla
     * {@code MeleeAttackGoal} paths to the target's own block and ends up body to body no matter how
     * far the mercenary can actually reach. Standing off at 0.7 of a reach that is itself a block
     * longer than vanilla's leaves a comfortable margin before the attack check fails - unlike the
     * first attempt at this, which held at the outer edge of an unextended reach and left the
     * mercenary unable to land anything unless something pinned it in place.
     */
    private static final double STANDOFF = 0.5D;

    /**
     * Stop shuffling this many ticks before the next swing is due, so the mercenary is planted rather
     * than drifting on the one tick the attack check actually runs.
     */
    private static final int STRAFE_STOP_BEFORE_SWING = 4;

    private final MercenaryEntity merc;
    // MeleeAttackGoal keeps its own copy private, and the strafe below stops the navigation it owns
    private final double speedModifier;

    private final MercenaryStrafe strafe = new MercenaryStrafe();

    public MercenaryMeleeAttackGoal(MercenaryEntity merc, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(merc, speedModifier, followingTargetEvenIfNotSeen);
        this.merc = merc;
        this.speedModifier = speedModifier;
    }

    /**
     * Vanilla's tick, plus a shuffle once the mercenary is actually in contact.
     * <p>
     * super runs first and keeps owning the walk in and the attack timer - taking that over would
     * mean reimplementing MeleeAttackGoal's repath pacing for no gain. The shuffle is layered on top
     * afterwards, and only in the one situation the mercenary used to stand perfectly still in:
     * pressed against its target with nothing else to do.
     * <p>
     * Closing distance always belongs to pathfinding, which is far faster than a strafe. Once the
     * mercenary is at its {@link #STANDOFF} it holds there and only sways, and it stops even that
     * shortly before each swing ({@link #STRAFE_STOP_BEFORE_SWING}), so the shuffle can never be the
     * reason a hit is missed.
     */
    @Override
    public void tick() {
        super.tick();

        LivingEntity target = merc.getTarget();
        if (target == null) {
            strafe.reset();
            return;
        }

        // a cast should read as committed, and MercenarySpellCaster owns the navigation while it
        // walks a skill into range - holding position here would smear that approach
        if (merc.isCastingSpell() || merc.isApproachingForCast()) {
            strafe.reset();
            return;
        }

        double reachSqr = getAttackReachSqr(target);
        double distSqr = merc.distanceToSqr(target);

        if (distSqr > reachSqr * STANDOFF) {
            strafe.reset();
            // still closing, and vanilla pathing owns that. it only repaths when the target itself
            // has moved though, and a previous tick's hold stopped whatever path it had - without
            // this the mercenary can stand there while its target strolls off.
            if (merc.getNavigation().isDone()) {
                merc.getNavigation().moveTo(target, speedModifier);
            }
            return;
        }

        // close enough. holding is the point - left alone, super keeps pathing to the target's own
        // block and the mercenary ends up hugging it.
        merc.getNavigation().stop();

        if (getTicksUntilNextAttack() <= STRAFE_STOP_BEFORE_SWING) {
            strafe.reset();
            return; // planted for the swing
        }

        // purely lateral: the standoff hold owns distance, so the sway has no forward component to
        // fight with - which is what the "sway, don't orbit" tuning wanted in the first place
        strafe.tick(merc, target, 0F);
    }

    /**
     * Vanilla's reach plus {@link #BONUS_REACH}, so a mercenary doesn't have to be nose to nose.
     * <p>
     * Derived from super rather than hard coded: vanilla's formula already scales with both bodies'
     * widths, and a mercenary should reach a block further than whatever that works out to against a
     * particular target. This is the one method both {@code checkAndPerformAttack} and
     * {@code canUse}'s no-path fallback consult, so overriding it covers everything.
     */
    @Override
    protected double getAttackReachSqr(LivingEntity target) {
        double reach = Math.sqrt(super.getAttackReachSqr(target)) + BONUS_REACH;
        return reach * reach;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
        if (merc.isCastingSpell()) {
            // deliberately without resetAttackCooldown(): the swing was postponed, not spent, so the
            // mercenary hits on the first tick after the cast rather than waiting another full second
            return;
        }
        super.checkAndPerformAttack(enemy, distToEnemySqr);
    }
}
