package com.robertx22.mine_and_slash.database.data.mercenary.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Giving ground: the half of "hold your distance" that isn't walking in. Shared by both combat goals
 * the way {@link MercenaryStrafe} is, and typed to {@code PathfinderMob} for the same reason -
 * nothing in here is companion specific.
 * <p>
 * Retreating is done by pathfinding rather than by backing up through {@link MercenaryStrafe} with a
 * negative forward input, and that isn't a style choice. {@code MoveControl.strafe} pins its own
 * speed modifier to 0.25, so a strafing retreat moves at a quarter of the mercenary's Move Speed -
 * anything chasing it at a full speed modifier simply walks it down and the distance is lost anyway.
 * The cost is that {@code MoveControl.MOVE_TO} turns the body toward where it is going, so a
 * retreating mercenary shows its back. Its head still tracks the target, because the goals set that
 * separately, and nothing about landing a hit depends on facing.
 */
public class MercenaryRetreat {

    /**
     * How long one retreat is allowed to run before a new destination is picked. Every moveTo to a
     * raw position is a fresh A* over a region snapshot, and the point being fled from slides along
     * with the mercenary, so PathNavigation's "same target as last time" cache almost never hits.
     * Re-pathing every tick would both cost real time and keep resetting the path out from under the
     * mercenary's feet.
     */
    private static final int REPATH_INTERVAL = 10;

    // where this helper last sent the mercenary, or null when the last attempt found nowhere to go.
    // the block form is kept alongside so the "is the navigation still running MY path" check below
    // is a comparison and not a rounding exercise - PathNavigation.getTargetPos returns the block the
    // live path ends at, which is exactly BlockPos.containing of what moveTo was handed.
    @Nullable
    private Vec3 dest;
    @Nullable
    private BlockPos destBlock;

    private int repathCooldown;

    /**
     * One tick of backing away.
     *
     * @param hold          how far the caller wants to end up from the target, in blocks. It sets the
     *                      search radius, so a short hold picks nearer positions.
     * @param speedModifier the goal's own movement speed, not a strafe's fixed quarter speed
     */
    public void tick(PathfinderMob merc, LivingEntity target, double hold, double speedModifier) {

        if (repathCooldown > 0) {
            repathCooldown--;
        }

        PathNavigation nav = merc.getNavigation();

        if (dest != null) {
            if (!nav.isDone() && repathCooldown > 0) {
                // a path is running and this helper's last order hasn't expired. either it is still
                // ours, in which case let it finish the way vanilla's own kiting goal
                // (AvoidEntityGoal) does, or another goal re-pathed on top of it.
                //
                // MeleeAttackGoal.tick is that other goal: it paths to the target every 4 to 11 ticks
                // whenever the target has moved a block, which anything chasing the mercenary always
                // has. Left alone that walks the mercenary straight back into what it is retreating
                // from for about half of every retreat. Restoring our own destination costs nothing
                // when the theft didn't happen - PathNavigation.createPath early outs when the live
                // path already targets the requested block - and one path find when it did.
                if (!destBlock.equals(nav.getTargetPos())) {
                    nav.moveTo(dest.x, dest.y, dest.z, speedModifier);
                }
                return;
            }
            // arrived, or the interval is up. fall through and pick the next hop immediately rather
            // than standing still waiting for a timer - a melee retreat is only a block or two, so
            // it finishes well inside REPATH_INTERVAL and the pause would read as a stutter.
        } else if (repathCooldown > 0) {
            // the last attempt found nowhere to go, which means cornered. this is the case the
            // interval is really for: getPosAway is ten random attempts of its own, and re-running it
            // every tick for a mercenary that is pinned against a wall is the most expensive thing
            // either combat goal could do.
            return;
        }

        repathCooldown = REPATH_INTERVAL;

        // unlike a raw "two blocks directly backwards" vector, this only ever returns somewhere the
        // mercenary can actually stand on and path to, so backing into terrain or off a ledge is out.
        Vec3 away = DefaultRandomPos.getPosAway(merc, Math.max(2, (int) hold), 4, target.position());

        // and refuse a "retreat" that doesn't actually gain any ground
        if (away == null || target.distanceToSqr(away) < target.distanceToSqr(merc)) {
            dest = null;
            destBlock = null;
            return;
        }

        dest = away;
        destBlock = BlockPos.containing(away);
        nav.moveTo(away.x, away.y, away.z, speedModifier);
    }

    /** call when the mercenary stops retreating, so a stale destination can't survive the next fight */
    public void reset() {
        dest = null;
        destBlock = null;
        repathCooldown = 0;
    }
}
