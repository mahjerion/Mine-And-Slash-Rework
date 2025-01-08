package com.robertx22.mine_and_slash.database.data.spells.components.actions;

import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.google.common.collect.Lists;
import com.robertx22.library_of_exile.utils.EntityUtils;
import com.robertx22.library_of_exile.utils.geometry.MyPosition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TeleportTargetToSourceAction extends SpellAction {

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
                teleportEntitySafe(x, ctx.sourceEntity.position());
            }
        });

    }

    private void teleportEntitySafe(LivingEntity entity, Vec3 destination) {

        Vec3 tpPosition = findSafeTeleportPosition(entity, destination).add(0.0, -entity.getBbHeight() / 2.0, 0.0);
        EntityUtils.setLoc(entity, new MyPosition(tpPosition).asVector3D(), entity.getYRot(), entity.getXRot());

    }

    // find a safe position to teleport to, getting as close to the destination as possible
    private Vec3 findSafeTeleportPosition(Entity entity, Vec3 destination) {

        Vec3 safePosition = findNearbySafeTeleportPosition(entity, destination);

        if (safePosition == null) {
            return destination;
        }

        // attempt to move the entity to the destination similar to Entity.collide
        AABB aabb = makeAabbForPosition(entity, safePosition);
        Vec3 delta = destination.subtract(safePosition);
        List<VoxelShape> entityCollisions = entity.level().getEntityCollisions(entity, aabb.expandTowards(delta));
        Vec3 clippedDelta = Entity.collideBoundingBox(entity, delta, aabb, entity.level(), entityCollisions);
        return safePosition.add(clippedDelta);

    }

    // only accurate to a block
    private Vec3 findNearbySafeTeleportPosition(Entity entity, Vec3 destination) {

        // search for a valid position in a cube surrounding the destination block
        Vec3 adjustedDestination = nudgePositionOutOfBlock(entity, destination);
        BlockPos blockPos = BlockPos.containing(adjustedDestination);
        // test with feet at the bottom of the destination block
        double yAdjust = entity.getBbHeight() / 2.0 - 0.5;

        for (Vec3i offset : SEARCH_OFFSETS) {
            Vec3 testPosition = blockPos.offset(offset).getCenter().add(0, yAdjust, 0);
            if (canEntityFit(entity, testPosition)) {
                return testPosition;
            }
        }

        return null;

    }

    // teleport projectiles end up at the exact edge of the block they hit, nudge into free space
    private Vec3 nudgePositionOutOfBlock(Entity entity, Vec3 destination) {

        final double NUDGE_SCALE = 1e-6;

        for (Vec3i offset : SEARCH_OFFSETS) {
            Vec3 testPosition = destination.add(new Vec3(offset.getX(), offset.getY(), offset.getZ()).scale(NUDGE_SCALE));
            if (canPointFit(entity, testPosition, NUDGE_SCALE)) {
                return testPosition;
            }
        }

        return destination;

    }

    private boolean canPointFit(Entity entity, Vec3 destination, double size) {
        return entity.level().noCollision(entity, AABB.ofSize(destination, size, size, size));
    }

    private boolean canEntityFit(Entity entity, Vec3 destination) {
        return entity.level().noCollision(entity, makeAabbForPosition(entity, destination));
    }

    private AABB makeAabbForPosition(Entity entity, Vec3 position) {
        return AABB.ofSize(position, entity.getBbWidth(), entity.getBbHeight(), entity.getBbWidth());
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
