package com.robertx22.mine_and_slash.database.data.spells.components;

import com.robertx22.library_of_exile.utils.geometry.MyPosition;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.AoeSelector;
import com.robertx22.mine_and_slash.database.data.spells.entities.CalculatedSpellData;
import com.robertx22.mine_and_slash.database.data.spells.entities.IDatapackProjectileEntity;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellUtils;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.EntityFinder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Vector3f;

public class ProjectileCastHelper {

    LivingEntity caster;

    public boolean silent = false;
    public float apart = 3;
    public float shootSpeed = 1;
    public int projectilesAmount = 1;
    public float randomSpreadDegrees = 0f;
    public boolean gravity = true;
    public int lifespanTicks = -1;
    EntityType projectile;
    CalculatedSpellData data;
    MapHolder holder;
    Vec3 pos;

    public CastType castType = CastType.SPREAD_OUT_IN_RADIUS;

    public enum CastType {
        SPREAD_OUT_IN_RADIUS, SPREAD_OUT_HORIZONTAL, SPREAD_OUT_CIRCLE
    }

    public float pitch;
    public float pitchOffset;

    public float yaw;
    public float yawOffset;

    public double offsetX;
    public double offsetY;
    public double offsetZ;

    public boolean fallDown = false;
    public boolean targetEnemy = false;
    public boolean targetCaster = false;

    SpellCtx ctx;

    public ProjectileCastHelper(SpellCtx ctx, Vec3 pos, MapHolder holder, LivingEntity caster, EntityType projectile, CalculatedSpellData data) {
        this.ctx = ctx;
        this.projectile = projectile;
        this.caster = caster;
        this.data = data;
        this.holder = holder;
        this.pos = pos;

        this.pitch = caster.getXRot();
        this.yaw = caster.getYRot();
    }

    public void cast() {

        if (data.data.getBoolean(EventData.NOVA) || this.holder.getOrDefault(MapField.NOVA, false)) {
            this.castType = CastType.SPREAD_OUT_CIRCLE;
        } else if (data.data.getBoolean(EventData.BARRAGE)) {
            this.castType = CastType.SPREAD_OUT_HORIZONTAL;
        }

        Level world = caster.level();

        LivingEntity target = null;

        // Find target once if targeting is enabled
        if (targetCaster) {
            target = caster;
        } else if (targetEnemy) {
            double radius = calculateRadius();
            double radiusSqr = radius * radius;
            EntityFinder.Setup<LivingEntity> finder = EntityFinder.start(caster, LivingEntity.class, pos.add(0, 0, 0))
                    .finder(EntityFinder.SelectionType.RADIUS)
                    .searchFor(AllyOrEnemy.enemies)
                    .predicate(e -> e.distanceToSqr(ctx.getPos()) <= radiusSqr && AoeSelector.canHit(ctx.getPos(), e))
                    .radius(radius);

            target = finder.getClosest();

            if (target == null) {
                return;
            }
        }

        if (target != null) {
            Vec3 baseDirection = positionToVelocity(new MyPosition(pos), new MyPosition(target.getEyePosition()));

            // Vec3.normalize() returns ZERO below 1e-4 length, meaning we spawned right on top of the target
            if (baseDirection.lengthSqr() > 0) {
                pitch = (float) Math.asin(-baseDirection.y) * Mth.RAD_TO_DEG;
                yaw = (float) Math.atan2(-baseDirection.x, baseDirection.z) * Mth.RAD_TO_DEG;
            }
        }

        for (int i = 0; i < projectilesAmount; i++) {
            float multiProjYawOffset = 0f;
            Vec3 posAdd = new Vec3(0, 0, 0);

            if (projectilesAmount > 1) {
                float offset = i - (float) (projectilesAmount - 1) / 2;

                if (this.castType == CastType.SPREAD_OUT_IN_RADIUS) {
                    // total cone is apart * (projectilesAmount - 1) / projectilesAmount
                    multiProjYawOffset = offset * apart / projectilesAmount;
                } else if (this.castType == CastType.SPREAD_OUT_HORIZONTAL) {
                    // 1m between each projectile
                    posAdd = getSideVelocity(caster).scale(offset);
                } else if (this.castType == CastType.SPREAD_OUT_CIRCLE) {
                    // just spread all the projectiles in a circle and reset the pitch, so it's always horizontal
                    multiProjYawOffset = i * (float) 360 / projectilesAmount;
                    pitch = 0;
                }
            }

            // Apply random spread offsets to yaw and pitch
            float randomYawOffset = 0f;
            float randomPitchOffset = 0f;
            if (randomSpreadDegrees > 0f) {
                randomYawOffset = (float) ((Math.random() * 2 - 1) * randomSpreadDegrees);
                randomPitchOffset = (float) ((Math.random() * 2 - 1) * randomSpreadDegrees);
            }

            // Apply offset from spell
            posAdd = posAdd.add(getSideVelocity(caster).scale(offsetX));
            posAdd = posAdd.add(caster.getUpVector(1f).scale(offsetY));
            posAdd = posAdd.add(caster.getViewVector(1f).scale(offsetZ));

            AbstractArrow en = (AbstractArrow) projectile.create(world);
            SpellUtils.setUpProjectilePosition(pos.add(posAdd), en, ctx.getPositionEntity());
            SpellUtils.initSpellEntity(en, caster, data, holder);

            float totalPitchOffset = pitchOffset + randomPitchOffset;
            float totalYawOffset = yawOffset + multiProjYawOffset + randomYawOffset;
            Vector3f upVector = new Vector3f();
            Vector3f direction = calculateDirection(pitch, yaw, totalPitchOffset, totalYawOffset, upVector);

            ((IDatapackProjectileEntity) en).setVectors(direction, upVector);

            en.shoot(direction.x, direction.y, direction.z, shootSpeed, 0f);

            if (fallDown) {
                en.setDeltaMovement(0, -1, 0);
            }

            en.setSilent(silent);
            caster.level().addFreshEntity(en);
        }

    }

    /**
     * Vanilla {@code AbstractArrow.tick()} scales velocity by this every tick in air, and
     * {@link com.robertx22.mine_and_slash.database.data.spells.entities.SimpleProjectileEntity}
     * never re-normalizes it, so a projectile does NOT travel {@code speed * lifeTicks} blocks.
     */
    public static final double AIR_DRAG_PER_TICK = 0.99D;

    /**
     * Real blocks a spell projectile covers before it expires: the geometric sum
     * {@code speed * (1 + 0.99 + 0.99^2 + ...)} over its lifespan. Linear in speed, so callers
     * may apply a projectile-speed multiplier to the result instead of the input.
     */
    public static double travelDistance(double speed, double lifeTicks) {
        if (lifeTicks <= 0 || speed <= 0) {
            return 0;
        }
        // java.lang.Math on purpose - this file imports JOML's Math, which has no pow()
        return speed * (1 - java.lang.Math.pow(AIR_DRAG_PER_TICK, lifeTicks)) / (1 - AIR_DRAG_PER_TICK);
    }

    private double calculateRadius() {
        if (lifespanTicks == -1) {
            return 15;
        }
        return travelDistance(shootSpeed, lifespanTicks);
    }

    private @NotNull Vector3f calculateDirection(float pitch, float yaw, float pitchOffset, float yawOffset, Vector3f outUpVector) {
        // Calculate direction vectors from pitch and yaw
        float pitchRad = (pitch + pitchOffset) * Mth.DEG_TO_RAD;
        float yawRad = yaw * Mth.DEG_TO_RAD;
        float yawOffsetRad = yawOffset * Mth.DEG_TO_RAD;

        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);

        Vector3f forward = new Vector3f(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
        outUpVector.set(-sinYaw * sinPitch, cosPitch, cosYaw * sinPitch);

        return forward.rotateAxis(-yawOffsetRad, outUpVector.x, outUpVector.y, outUpVector.z);
    }

    public static Vec3 positionToVelocity(MyPosition current, MyPosition destination) {
        return destination.subtract(current).normalize();
    }

    public Vec3 getSideVelocity(Entity shooter) {
        float yaw = shooter.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
    }

}

