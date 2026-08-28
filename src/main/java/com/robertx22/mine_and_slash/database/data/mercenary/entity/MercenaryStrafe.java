package com.robertx22.mine_and_slash.database.data.mercenary.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * The small side to side shuffle a mercenary does while it is standing in front of something with
 * nothing to do. Shared by both combat goals: the melee one uses it in contact with its target, the
 * ranged one inside its hold band, and neither of them looked alive without it.
 * <p>
 * Deliberately a sway, not an orbit. The sidestep input is small and the direction reverses every
 * few ticks, so the lateral travel cancels itself out and the mercenary stays over roughly one spot
 * instead of walking a lap around its target - which would tow the mob around the arena and drag it
 * across the owner.
 */
public class MercenaryStrafe {

    /** how hard to step sideways. small on purpose - see the class note about swaying, not orbiting */
    private static final float SIDESTEP = 0.2F;

    /**
     * How long one direction lasts. Short, and always reversed rather than reversed on a chance: a
     * per-second coin flip lets the mercenary travel in one direction for several seconds, which is
     * exactly the lap this is trying to avoid. The interval is re-rolled on each flip so two
     * mercenaries standing together don't sway in lockstep.
     */
    private static final int MIN_FLIP_TICKS = 8;
    private static final int MAX_FLIP_TICKS = 14;

    private boolean clockwise;
    private int ticksLeft;

    /** picks a new direction when the current one has run its course. call once per strafing tick */
    private void advance(RandomSource random) {
        if (ticksLeft > 0) {
            ticksLeft--;
            return;
        }
        clockwise = !clockwise;
        ticksLeft = MIN_FLIP_TICKS + random.nextInt(MAX_FLIP_TICKS - MIN_FLIP_TICKS + 1);
    }

    /**
     * One tick of shuffling in place.
     *
     * @param forward positive to close on the target, negative to give it room, 0 to hold. the melee
     *                goal uses it to stay inside its attack reach; the ranged goal owns distance
     *                through its own band and passes 0.
     */
    public void tick(MercenaryEntity merc, LivingEntity target, float forward) {

        // required, not optional. Mob.serverAiStep runs the goals BEFORE navigation.tick(), so an
        // active path overwrites the strafe operation on MoveControl every tick and nothing moves.
        merc.getNavigation().stop();

        advance(merc.getRandom());

        // MoveControl's strafe reads the mercenary's body yaw, not its head. getLookControl().setLookAt
        // only turns the head, so without this the sidestep goes off in whatever direction the body
        // happened to be left facing instead of around the target. RangedBowAttackGoal pairs these two
        // the same way.
        merc.lookAt(target, 30F, 30F);

        // strafe() pins its own speed modifier to 0.25, so this is a shuffle rather than a sprint, and
        // it still runs through MoveControl - which means Move Speed and any slow on the mercenary
        // both apply, where moving it by setDeltaMovement would bypass them.
        merc.getMoveControl().strafe(forward, clockwise ? SIDESTEP : -SIDESTEP);
    }

    /** call when the mercenary stops strafing, so the next bout starts on a fresh direction timer */
    public void reset() {
        ticksLeft = 0;
    }
}
