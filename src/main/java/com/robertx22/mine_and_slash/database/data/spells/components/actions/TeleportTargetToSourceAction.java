package com.robertx22.mine_and_slash.database.data.spells.components.actions;

import com.google.common.collect.Lists;
import com.robertx22.library_of_exile.utils.EntityUtils;
import com.robertx22.library_of_exile.utils.geometry.MyPosition;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TeleportTargetToSourceAction extends SpellAction {

    // how far ahead of the caster enemies pulled to them land. aiming at the caster's own position
    // drops mobs inside them, and vanilla's push-apart then scatters them in an arbitrary direction,
    // so roughly half end up behind the player.
    private static final double PULL_FRONT_DISTANCE = 2.0;

    // granularity of the walk back from an unreachable destination, and a ceiling so a very long
    // teleport can't turn one failed placement into hundreds of collision checks
    private static final double RETREAT_STEP = 0.5;
    private static final int MAX_RETREAT_STEPS = 64;

    // search used to find a safe position near the destination
    private static final List<Vec3i> SEARCH_OFFSETS = Lists.newArrayList();

    static {
        final int SEARCH_RADIUS = 1;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    SEARCH_OFFSETS.add(new Vec3i(x, y, z));
                }
            }
        }

        SEARCH_OFFSETS.sort((a, b) -> {
            // sort by horizontal distance, then by vertical distance
            int aDistance = Math.abs(a.getX()) + Math.abs(a.getZ());
            int bDistance = Math.abs(b.getX()) + Math.abs(b.getZ());

            if (aDistance == bDistance) {
                return Math.abs(a.getY()) - Math.abs(b.getY());
            } else {
                return aDistance - bDistance;
            }
        });
    }

    public TeleportTargetToSourceAction() {
        super(Arrays.asList());
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {
        targets.forEach(x -> {
            if (x.level() == ctx.sourceEntity.level()) { // don't allow teleport in wrong dimension
                teleportEntitySafe(x, destinationFor(ctx, x));
            }
        });
    }

    // enemies yanked to the caster land in front of them instead of on top of them. only when the
    // caster is the destination: SpellCtx.onCast sets sourceEntity to the caster, while onExpire /
    // onTick / onHit set it to the spell entity, so pulls toward a projectile or block (black hole,
    // cyclone, traps) and self-teleports (dashes, blinks) keep aiming at the entity exactly.
    private static Vec3 destinationFor(SpellCtx ctx, LivingEntity target) {
        if (ctx.sourceEntity == ctx.caster && AllyOrEnemy.enemies.is(ctx.caster, target)) {
            return pullDestination(ctx.caster, target);
        }
        return ctx.sourceEntity.position();
    }

    // picks WHERE in front of the caster to aim, walking the offset back a block at a time until
    // the target has somewhere it fits - a caster stood flush against a wall shouldn't drag mobs
    // toward the inside of it. the caster's own position is the last resort: something is already
    // standing there. reachability of whatever this returns is teleportEntitySafe's problem.
    static Vec3 pullDestination(LivingEntity caster, LivingEntity target) {
        for (double dist = PULL_FRONT_DISTANCE; dist > 0; dist -= 1.0) {
            Vec3 candidate = inFrontOf(caster, dist);
            if (findNearbySafeTeleportPosition(target, candidate) != null) {
                return candidate;
            }
        }
        return caster.position();
    }

    // a point `distance` blocks ahead of where the entity faces, at the same height. yaw only, so
    // looking up or down can't fling pulled mobs into the air or bury them.
    private static Vec3 inFrontOf(LivingEntity en, double distance) {
        float yawRad = en.getYRot() * Mth.DEG_TO_RAD;
        return en.position()
                .add(-Mth.sin(yawRad) * distance, 0, Mth.cos(yawRad) * distance);
    }

    // every position below is a FEET position - what Entity.position() returns and what setLoc
    // wants. these used to be a mix of feet and bbox-centre coordinates, which biased the slide
    // below (and the old give-up branch) half an entity height downward, sinking things into floors.
    public static void teleportEntitySafe(LivingEntity entity, Vec3 destination) {
        Vec3 tpPosition = findSafeTeleportPosition(entity, destination);

        if (tpPosition == null) {
            return; // nowhere along the way it fits, leave it where it is rather than bury it
        }

        EntityUtils.setLoc(entity, new MyPosition(tpPosition).asVector3D(), entity.getYRot(), entity.getXRot());
    }

    // find a safe position to teleport to, getting as close to the destination as possible.
    // null means there is nowhere at all, and the caller must not move the entity.
    private static Vec3 findSafeTeleportPosition(Entity entity, Vec3 destination) {

        Vec3 safePosition = findNearbySafeTeleportPosition(entity, destination);

        if (safePosition == null) {
            return retreatTowardEntity(entity, destination);
        }

        // attempt to move the entity to the destination similar to Entity.collide
        AABB aabb = makeAabbForPosition(entity, safePosition);
        Vec3 delta = destination.subtract(safePosition);
        List<VoxelShape> entityCollisions = entity.level().getEntityCollisions(entity, aabb.expandTowards(delta));
        Vec3 clippedDelta = Entity.collideBoundingBox(entity, delta, aabb, entity.level(), entityCollisions);
        return safePosition.add(clippedDelta);

    }

    // nothing fits at the destination - walk back toward where the entity already stands and take
    // the furthest point along that line it fits at. a dash into a wall carries you up to the wall
    // instead of into it. the entity's own position is the far end of the walk and something is
    // already standing there, so this can only fail if it was clipping geometry to begin with.
    // no raycast on purpose: AoeSelector warns a raw level().clip here reintroduces the server
    // hang and has to go through MapChunkRaycastGuard. these are the same cheap AABB checks used
    // above, and they only ever run once the nearby search has already failed.
    private static Vec3 retreatTowardEntity(Entity entity, Vec3 destination) {

        Vec3 origin = entity.position();
        double distance = origin.distanceTo(destination);

        if (distance < 1.0E-4) {
            return canEntityFit(entity, origin) ? origin : null;
        }

        int steps = Mth.clamp((int) Math.ceil(distance / RETREAT_STEP), 1, MAX_RETREAT_STEPS);

        for (int i = 1; i <= steps; i++) {
            Vec3 testPosition = destination.lerp(origin, (double) i / steps);
            if (canEntityFit(entity, testPosition)) {
                return testPosition;
            }
        }

        return null;

    }

    // only accurate to a block
    private static Vec3 findNearbySafeTeleportPosition(Entity entity, Vec3 destination) {

        // search for a valid position in a cube surrounding the destination block
        Vec3 adjustedDestination = nudgePositionOutOfBlock(entity, destination);
        BlockPos blockPos = BlockPos.containing(adjustedDestination);

        for (Vec3i offset : SEARCH_OFFSETS) {
            // feet at the bottom of the candidate block
            Vec3 testPosition = Vec3.atBottomCenterOf(blockPos.offset(offset));
            if (canEntityFit(entity, testPosition)) {
                return testPosition;
            }
        }

        return null;

    }

    // teleport projectiles end up at the exact edge of the block they hit, nudge into free space
    private static Vec3 nudgePositionOutOfBlock(Entity entity, Vec3 destination) {

        final double NUDGE_SCALE = 1e-6;

        for (Vec3i offset : SEARCH_OFFSETS) {
            Vec3 testPosition = destination.add(new Vec3(offset.getX(), offset.getY(), offset.getZ()).scale(NUDGE_SCALE));
            if (canPointFit(entity, testPosition, NUDGE_SCALE)) {
                return testPosition;
            }
        }

        return destination;

    }

    private static boolean canPointFit(Entity entity, Vec3 destination, double size) {
        return entity.level().noCollision(entity, AABB.ofSize(destination, size, size, size));
    }

    private static boolean canEntityFit(Entity entity, Vec3 destination) {
        return entity.level().noCollision(entity, makeAabbForPosition(entity, destination));
    }

    // position is the entity's feet, so build the box the way vanilla does rather than centring it
    private static AABB makeAabbForPosition(Entity entity, Vec3 position) {
        return entity.getDimensions(entity.getPose()).makeBoundingBox(position);
    }

    public MapHolder create() {
        MapHolder c = new MapHolder();
        c.type = GUID();
        this.validate(c);
        return c;
    }

    @Override
    public String GUID() {
        return "tp_target_to_self";
    }

}
