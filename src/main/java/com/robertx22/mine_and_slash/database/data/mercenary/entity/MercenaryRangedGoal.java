package com.robertx22.mine_and_slash.database.data.mercenary.entity;

import com.robertx22.mine_and_slash.database.data.mercenary.MercenarySpellCaster;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.util.Mth;

import java.util.EnumSet;

/**
 * The combat goal a "ranged" {@link com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass.AiBehavior}
 * mercenary (e.g. the elementalist) uses instead of {@link net.minecraft.world.entity.ai.goal.MeleeAttackGoal}.
 * <p>
 * Keeps its distance from the target rather than closing in - the real damage for a caster comes from
 * {@link MercenarySpellCaster}, which runs independently of movement and casts regardless of range - and
 * only throws a melee hit when the target ends up inside melee reach anyway. That covers a target that
 * corners the mercenary despite the kiting.
 * <p>
 * Between skill cooldowns it fires whatever basic attack its weapon offers: an arrow from a bow, or
 * the skill the weapon's own class grants - a staff's Bolt, see
 * {@link MercenaryEntity#weaponBasicAttackSpell()}. Before that existed a staff mercenary had neither,
 * and simply stood at its kite distance doing nothing.
 */
public class MercenaryRangedGoal extends Goal {

    // roughly what vanilla's own width-scaled reach works out to against a normal sized target.
    // MercenaryMeleeAttackGoal derives its number from super instead, which this goal has no super
    // to ask - so the mercenary's weapon bonus is added to this stand-in rather than to nothing.
    private static final double VANILLA_MELEE_REACH = 2.0D;

    // how far the opportunistic poke below connects: the same bonus a melee mercenary gets, so a
    // kiting one doesn't have to be pressed against something to swing at it, and a two hander
    // reaches further here too.
    private double meleePokeReachSqr() {
        double reach = VANILLA_MELEE_REACH + merc.bonusMeleeReach();
        return reach * reach;
    }

    // deliberately NOT the poke reach. this is the floor of holdDistance(), and raising it to match
    // would push a short ranged skill out of its own range - anything sitting on MercenarySpellCaster's
    // MIN_CAST_RANGE floor of 2.5 would be clamped out to 3 and never connect.
    private static final double MIN_HOLD_DISTANCE = 2.0D;

    // the distance the mercenary holds when it has no skill in progress telling it otherwise
    private static final double KITE_DISTANCE = 8.0D;

    // the band is held, not a line: flee below hold * FLEE_BAND, close back in past hold, and do
    // nothing in between. a single threshold has the mercenary crossing it and reversing every tick.
    private static final double FLEE_BAND = 0.75D;

    private static final int MELEE_ATTACK_INTERVAL = 20;
    private static final int RANGED_ATTACK_INTERVAL = 20;

    private final MercenaryEntity merc;
    private final double speedModifier;
    private final MercenaryStrafe strafe = new MercenaryStrafe();
    private final MercenaryRetreat retreat = new MercenaryRetreat();
    private int meleeCooldown;
    private int rangedCooldown;

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
        retreat.reset();
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

        double hold = holdDistance();
        double holdSqr = hold * hold;

        // MercenarySpellCaster owns the navigation while it walks a skill into range. without this
        // the retreat below undoes every step on the tick after it is taken, and a kiting caster
        // can never close on anything - its short range skills would be unusable by design.
        if (!merc.isApproachingForCast()) {
            if (distSqr < holdSqr * FLEE_BAND * FLEE_BAND) {
                strafe.reset();
                retreat.tick(merc, target, hold, speedModifier);
            } else if (distSqr > holdSqr) {
                strafe.reset();
                retreat.reset();
                merc.getNavigation().moveTo(target, speedModifier);
            } else if (!merc.isCastingSpell()) {
                // parked at the distance it wants, waiting on cooldowns. sway rather than stand
                // rigid - the band above already owns the distance, so this is purely lateral
                retreat.reset();
                strafe.tick(merc, target, 0F);
            }
        } else {
            strafe.reset();
            retreat.reset();
        }

        // both attacks are opportunistic and neither one touches navigation. melee used to be an arm
        // of the movement decision above and stopped the navigation outright, which is what pinned a
        // mercenary in melee the moment anything cornered it - it stopped walking and never kited again.
        if (distSqr <= meleePokeReachSqr()) {
            meleeAttack(target);
        }

        // a bow shot, or the ranged basic attack the mercenary's weapon class grants it - a staff's
        // Bolt. performRangedAttack is still a no-op for a weapon that offers neither, which is the
        // fallback wanted for a caster holding something odd: keep kiting and let its skills work.
        if (merc.getSensing().hasLineOfSight(target)) {
            tryRangedAttack(target);
        }

        if (meleeCooldown > 0) {
            meleeCooldown--;
        }
        if (rangedCooldown > 0) {
            rangedCooldown--;
        }
    }

    /**
     * How far the mercenary wants to stand from its target right now.
     * <p>
     * A skill being cast or walked into range sets this to its own reach, so a five block skill is
     * held at five blocks rather than at the default kite distance - otherwise the mercenary closes
     * in for a short ranged skill and then flees back out of it before the cast even finishes.
     * Clamped at both ends: never inside melee reach, never further out than it would kite anyway.
     */
    private double holdDistance() {
        double active = merc.getActiveEngageRange();
        if (active <= 0) {
            return KITE_DISTANCE;
        }
        return Mth.clamp(active, MIN_HOLD_DISTANCE, KITE_DISTANCE);
    }

    private void meleeAttack(LivingEntity target) {
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

    private void tryRangedAttack(LivingEntity target) {
        if (rangedCooldown > 0) {
            return;
        }
        // performRangedAttack refuses mid cast anyway - the point of checking here as well is not to
        // burn the interval on a shot that never left the bow
        if (merc.isCastingSpell()) {
            return;
        }
        // a weapon granted basic attack - a staff's Bolt - is a short lived projectile, unlike an
        // arrow. hold fire past its reach rather than throwing one that expires in mid air. an
        // actual bow has no such limit and is left alone.
        Spell basic = merc.weaponBasicAttackSpell();
        if (basic != null) {
            double range = MercenarySpellCaster.basicAttackRange(merc, basic);
            if (merc.distanceToSqr(target) > range * range) {
                // spend the interval anyway. basicAttackRange samples a stat event to read the
                // mercenary's Projectile Speed, so re-asking every tick while a target sits just out
                // of reach would be the most expensive thing this goal does.
                rangedCooldown = RANGED_ATTACK_INTERVAL;
                return;
            }
        }
        merc.performRangedAttack(target, 1.0F);
        rangedCooldown = RANGED_ATTACK_INTERVAL;
    }
}
