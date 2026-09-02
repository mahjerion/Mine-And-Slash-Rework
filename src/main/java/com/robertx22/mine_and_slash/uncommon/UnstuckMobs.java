package com.robertx22.mine_and_slash.uncommon;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

import java.util.Arrays;
import java.util.List;

/**
 * Last resort for an entity that was placed inside blocks: nudge it to the nearest spot where it
 * fits. Callers should try to pick a real spawn position first - see DungeonAddonUtil.placeEncounterMob.
 * <p>
 * This deliberately does NOT kill an entity it cannot free. It used to do
 * {@code hurt(fellOutOfWorld, 999999)} followed by {@code discard()}, and at the dungeon mob spawn
 * call site that runs BEFORE the mob is added to the level - so the mob was killed while nobody could
 * see it, yet the hurt and death sounds are broadcast by position rather than by entity, and vanilla
 * {@code dropAllDeathLoot} still ran. Players heard a burst of mobs dying with nothing there, picked up
 * vanilla junk, and got no Mine and Slash loot (the mob's rarity and stats are applied after the spawn,
 * and "the void" is not a killer the loot roll can credit). It also ran the whole death chain -
 * OnMobDeathDrops, both LivingDeathEvent handlers, MercenaryKillCreditMixin - for a mob that never existed,
 * and left the dungeon completion denominator counting mobs that could never be killed.
 */
public class UnstuckMobs {

    static List<Direction> dirs = Arrays.asList(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN);

    // how many blocks out to probe in each direction. escaping a wall means passing through it, so a
    // solid block on the way out is skipped rather than ending the search in that direction
    private static final int MAX_NUDGE = 3;

    /**
     * @return true when the entity is free - including when it was never stuck. false means nothing
     * within {@link #MAX_NUDGE} blocks worked and the entity has been put back where it started;
     * it is up to the caller whether to keep it, discard it, or leave it be.
     */
    public static boolean unstuckFromWalls(Entity en) {
        if (!en.isInWall()) {
            return true;
        }

        // capture the start once: every probe below teleports the entity, so reading blockPosition()
        // inside the loop measured each candidate from wherever the previous one happened to land,
        // which turned this into a random walk instead of a search
        BlockPos origin = en.blockPosition();

        for (Direction dir : dirs) {
            for (int i = 1; i <= MAX_NUDGE; i++) {
                BlockPos tp = origin.relative(dir, i);
                if (en.level().getBlockState(tp).isSolid()) {
                    continue;
                }
                // centre of the block, not its corner - a corner placement leaves a normal hitbox
                // overlapping the two neighbouring columns and reads as in a wall again
                en.teleportTo(tp.getX() + 0.5, tp.getY(), tp.getZ() + 0.5);
                if (!en.isInWall()) {
                    return true;
                }
            }
        }

        en.teleportTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        // this used to announce itself by killing the entity, which is exactly the bug. Nothing is
        // visible now, so leave a dev-only trace - a room shape that fails this often is worth knowing
        // about, and it is the one signal left that a spawn spot was bad.
        if (MMORPG.RUN_DEV_TOOLS) {
            ExileLog.get().warn("Could not free " + en.getType() + " from blocks at " + origin);
        }
        return false;
    }
}
