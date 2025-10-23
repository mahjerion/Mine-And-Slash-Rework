package com.robertx22.mine_and_slash.database.data.spells.components;

import com.robertx22.library_of_exile.utils.geometry.MyPosition;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.AoeSelector;
import com.robertx22.mine_and_slash.database.data.spells.entities.CalculatedSpellData;
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
        SPREAD_OUT_IN_RADIUS, SPREAD_OUT_HORIZONTAL
    }

    public float pitch;

    public float yaw;

    public boolean fallDown = false;
    public boolean targetEnemy = false;

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

        if (data.data.getBoolean(EventData.BARRAGE)) {
            this.castType = CastType.SPREAD_OUT_HORIZONTAL;
        }

        Level world = caster.level();

        LivingEntity target = null;
        Vec3 baseDirection = null;

        // Find target once if targeting is enabled
        if (targetEnemy) {
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

            baseDirection = positionToVelocity(new MyPosition(pos), new MyPosition(target.getEyePosition()));

            pitch = (float) Math.toDegrees(Math.asin(-baseDirection.y));
            yaw = (float) Math.toDegrees(Math.atan2(-baseDirection.x, baseDirection.z));
        }

        for (int i = 0; i < projectilesAmount; i++) {
            float addYaw = 0;
            Vec3 posAdd = new Vec3(0, 0, 0);

            if (projectilesAmount > 1) {
                float offset = i - (float) (projectilesAmount - 1) / 2;

                if (this.castType == CastType.SPREAD_OUT_IN_RADIUS) {
                    // total cone is apart * (projectilesAmount - 1) / projectilesAmount
                    addYaw = offset * apart / projectilesAmount;
                } else if (this.castType == CastType.SPREAD_OUT_HORIZONTAL) {
                    // 1m between each projectile
                    posAdd = getSideVelocity(caster).multiply(offset, offset, offset);
                }
            }

            // Apply random spread offsets to yaw and pitch
            float randomYawOffset = 0f;
            float randomPitchOffset = 0f;
            if (randomSpreadDegrees > 0f) {
                randomYawOffset = (float) ((Math.random() * 2 - 1) * randomSpreadDegrees);
                randomPitchOffset = (float) ((Math.random() * 2 - 1) * randomSpreadDegrees);
            }

            AbstractArrow en = (AbstractArrow) projectile.create(world);
            SpellUtils.setUpProjectilePosition(pos.add(posAdd), en, ctx.getPositionEntity());
            SpellUtils.initSpellEntity(en, caster, data, holder);

            Vector3f direction = calculateDirection(pitch, yaw, randomPitchOffset, addYaw, randomYawOffset);
            en.shoot(direction.x, direction.y, direction.z, shootSpeed, 0f);

            if (fallDown) {
                en.setDeltaMovement(0, -1, 0);
            }

            en.setSilent(silent);
            caster.level().addFreshEntity(en);
        }

    }

    private double calculateRadius() {
        if (lifespanTicks == -1) {
            return 15;
        }
        return lifespanTicks * shootSpeed;
    }

    private @NotNull Vector3f calculateDirection(float pitch, float yaw, float randomPitchOffset, float addYaw, float randomYawOffset) {
        // Calculate direction vectors from pitch and yaw
        float pitchRad = Math.toRadians(pitch);
        float yawRad = Math.toRadians(yaw);

        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);

        Vector3f forward = new Vector3f(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
        Vector3f right = new Vector3f(cosYaw, 0f, sinYaw);
        Vector3f up = new Vector3f(-sinYaw * sinPitch, cosPitch, cosYaw * sinPitch);

        float pitchOffset = Math.toRadians(randomPitchOffset);
        float yawOffset = Math.toRadians(addYaw + randomYawOffset);

        return forward.rotateAxis(pitchOffset, right.x, right.y, right.z).rotateAxis(-yawOffset, up.x, up.y, up.z);
    }

    public static Vec3 positionToVelocity(MyPosition current, MyPosition destination) {
        return destination.subtract(current).normalize();
    }

    public Vec3 getSideVelocity(Entity shooter) {
        float yaw = shooter.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
    }

}

