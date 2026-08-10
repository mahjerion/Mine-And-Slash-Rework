package com.robertx22.mine_and_slash.mixin_methods;

import com.robertx22.library_of_exile.dimension.MapDimensions;
import com.robertx22.library_of_exile.main.ExileLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stops a raycast inside a map dimension from force-generating chunks.
 * <p>
 * Vanilla's {@code Level.clip} walks the segment calling {@code getBlockState}, which calls
 * {@code getChunk(x, z, FULL, true)} - and that <b>blocks the server thread until the chunk has
 * finished generating</b>. In the overworld that costs a moment. In a map dimension generating a
 * chunk means running the whole map chunk gen event (four structures, and for an instance whose map
 * data can't be read, a full dungeon room grid build), and it cascades into loading further chunks.
 * A single projectile tick could therefore freeze the server for minutes - which is exactly what the
 * ServerHangWatchdog killed the live server for, twice, from
 * {@code ThrowableProjectile.tick -> ProjectileUtil -> clip -> getChunkBlocking}.
 * <p>
 * The fix is to refuse the load rather than to make it faster. An ungenerated chunk in a map
 * dimension is <b>solid bedrock</b> ({@code MapChunkGenerator.fillFromNoise} fills it that way), so
 * reporting a block hit at the edge of the loaded region is not an approximation of what is there -
 * it is what is there. A projectile stops against bedrock either way; the difference is only whether
 * the server had to build the room behind it first.
 * <p>
 * Deliberately scoped to map dimensions. In the overworld an unloaded chunk is real terrain that
 * hasn't been built yet, and silently walling ender pearls at the frontier would be a behaviour
 * change nobody asked for.
 */
public class MapChunkRaycastGuard {

    // blocks between samples along the ray. a chunk is 16 wide, so 8 cannot step over one except by
    // clipping a corner - and missing a corner only means we let vanilla handle it, which is the old
    // behaviour, never worse.
    private static final double STEP = 8;

    // nothing in this game legitimately raycasts this far, and a projectile whose velocity has blown
    // up is the most likely source of one that does. The hang log showed generation marching 1440
    // blocks along +Z, out of one dungeon instance and into the next one, which is what this bounds.
    private static final double MAX_RAY_LENGTH = 512;

    // dimension membership is fixed once MapDimensions.register has run during mod construction, and
    // MapDimensions.getInfo builds a String every call (ResourceLocation.toString) - not something to
    // do once per projectile per tick.
    private static final Map<ResourceKey<Level>, Boolean> IS_MAP_DIM = new ConcurrentHashMap<>();

    private static final Map<ResourceKey<Level>, Long> LAST_LOG = new ConcurrentHashMap<>();
    private static final long LOG_COOLDOWN_MS = 30_000;
    private static final AtomicLong CLAMPED = new AtomicLong();

    /**
     * Whether raycasts in this level have to be guarded at all.
     * <p>
     * Memoized on the level key but answered by {@code MapDimensions.getInfo}, which matches on the
     * dimension TYPE id rather than the dimension id. That is the library's own convention -
     * MapDataFinder resolves map data through the same lookup - so this deliberately agrees with it
     * rather than being stricter. The consequence, if anyone ever hits it: a dimension that borrows a
     * map's dimension type would be guarded too.
     */
    public static boolean guards(Level level) {
        if (level == null || level.isClientSide) {
            return false;
        }
        return IS_MAP_DIM.computeIfAbsent(level.dimension(), k -> MapDimensions.getInfo(level) != null);
    }

    /**
     * Drop-in replacement for {@code level.clip(ctx)} that never generates a chunk in a map dimension.
     */
    public static BlockHitResult clip(Level level, ClipContext ctx) {
        if (guards(level)) {
            BlockHitResult wall = wallIfUnloaded(level, ctx.getFrom(), ctx.getTo());
            if (wall != null) {
                return wall;
            }
        }
        return level.clip(ctx);
    }

    /**
     * A block hit at the point where the segment leaves the loaded region, or null when the whole
     * segment is loaded and the caller should just run vanilla.
     * <p>
     * The result is deliberately {@code Type.BLOCK} rather than a miss: callers branch on
     * {@code != MISS} to mean "something stopped this", which is precisely what the bedrock beyond
     * the frontier does. Returning a miss instead would let projectiles sail on and hit the same
     * un-generated chunk again next tick.
     */
    @Nullable
    public static BlockHitResult wallIfUnloaded(Level level, Vec3 from, Vec3 to) {
        try {
            if (!isFinite(from) || !isFinite(to)) {
                // a NaN/infinite endpoint means the entity's motion is already corrupt. vanilla would
                // walk this forever; stop it where it stands.
                return wallAt(level, from, from, to);
            }

            double dx = to.x - from.x;
            double dy = to.y - from.y;
            double dz = to.z - from.z;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);

            var source = level.getChunkSource();

            if (len < 1.0E-7) {
                return source.hasChunk(SectionPos.blockToSectionCoord(from.x), SectionPos.blockToSectionCoord(from.z))
                        ? null : wallAt(level, from, from, to);
            }

            double limit = Math.min(len, MAX_RAY_LENGTH);
            Vec3 dir = new Vec3(dx / len, dy / len, dz / len);

            Vec3 lastLoaded = from;
            for (double t = 0; ; t += STEP) {
                if (t > limit) {
                    t = limit;
                }
                Vec3 at = from.add(dir.scale(t));
                if (!source.hasChunk(SectionPos.blockToSectionCoord(at.x), SectionPos.blockToSectionCoord(at.z))) {
                    return wallAt(level, lastLoaded, from, to);
                }
                lastLoaded = at;
                if (t >= limit) {
                    break;
                }
            }

            // every chunk we were willing to look at is loaded. if the ray was longer than we were
            // willing to look, stop it at the cap rather than handing vanilla the rest of it.
            return limit < len ? wallAt(level, lastLoaded, from, to) : null;

        } catch (Exception e) {
            // a throw from here lands in the middle of an entity tick, which would be worse than the
            // hang this exists to prevent. let vanilla have it.
            e.printStackTrace();
            return null;
        }
    }

    private static BlockHitResult wallAt(Level level, Vec3 hit, Vec3 from, Vec3 to) {
        Direction face = Direction.getNearest(to.x - from.x, to.y - from.y, to.z - from.z).getOpposite();
        log(level, hit);
        return new BlockHitResult(hit, face, BlockPos.containing(hit), false);
    }

    private static boolean isFinite(Vec3 v) {
        return v != null
                && Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    // one line per dimension per cooldown, with a running total. the total is what makes this a rate
    // rather than an anecdote - it's how you tell "the fix is working" from "it just hasn't happened
    // again yet". No per entity state is kept.
    private static void log(Level level, Vec3 at) {
        long total = CLAMPED.incrementAndGet();
        long now = System.currentTimeMillis();
        Long last = LAST_LOG.get(level.dimension());
        if (last != null && now - last < LOG_COOLDOWN_MS) {
            return;
        }
        LAST_LOG.put(level.dimension(), now);
        // "since startup", not "since server start": these statics live as long as the JVM, so in
        // singleplayer the count carries across world loads.
        ExileLog.get().warn("Clamped a raycast in " + level.dimension().location() + " at "
                + BlockPos.containing(at) + ": it left the loaded region and would have force-generated"
                + " map chunks from inside the entity tick. Treated the frontier as the bedrock it is."
                + " Total since startup: " + total + ".");
    }
}
