package com.robertx22.mine_and_slash.database.data.mercenary.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Vanilla's {@link MeleeAttackGoal}, minus the swing while the mercenary is mid spell cast.
 * <p>
 * The goal is only overridden at the attack, never at {@code canUse}: it also owns the pathing that
 * closes the mercenary in on its target, and taking the whole goal away for the length of a wind up
 * would both stop it dead and hand the MOVE flag to RandomStrollGoal, which would walk it off to
 * wander mid fight. Blocking {@code doHurtTarget} on the entity instead isn't enough either - this is
 * the method that swings the arm, so that route leaves the mercenary miming hits that do nothing.
 */
public class MercenaryMeleeAttackGoal extends MeleeAttackGoal {

    private final MercenaryEntity merc;

    public MercenaryMeleeAttackGoal(MercenaryEntity merc, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(merc, speedModifier, followingTargetEvenIfNotSeen);
        this.merc = merc;
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
