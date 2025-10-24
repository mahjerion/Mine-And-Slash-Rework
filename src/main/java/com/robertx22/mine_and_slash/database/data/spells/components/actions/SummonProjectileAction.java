package com.robertx22.mine_and_slash.database.data.spells.components.actions;

import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.ProjectileCastHelper;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashEntities;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class SummonProjectileAction extends SpellAction {

    public SummonProjectileAction() {
        super(Arrays.asList(MapField.ENTITY_NAME, MapField.PROJECTILE_COUNT, MapField.ITEM, MapField.PROJECTILE_SPEED, MapField.LIFESPAN_TICKS, MapField.PROJECTILE_SPREAD_RANDOMNESS));
    }

    public enum ShootWay {
        FROM_PLAYER_VIEW, DOWN, FIND_ENEMY
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {
        if (ctx.world.isClientSide) {
            return;
        }

        Optional<EntityType<?>> projectile = EntityType.byString(data.get(MapField.PROJECTILE_ENTITY));

        PositionSource posSource = data.getOrDefault(PositionSource.CASTER);
        ShootWay shootWay = data.getOrDefault(ShootWay.FROM_PLAYER_VIEW);

        Vec3 pos = ctx.getPos();

        boolean silent = data.getOrDefault(MapField.IS_SILENT, false);

        ProjectileCastHelper builder = new ProjectileCastHelper(ctx, pos, data, ctx.caster, projectile.get(), ctx.calculatedSpellData);
        builder.projectilesAmount = (int) (data.get(MapField.PROJECTILE_COUNT) + ctx.calculatedSpellData.data.getNumber(EventData.BONUS_PROJECTILES, 0).number);

        builder.silent = silent;

        builder.shootSpeed = data.get(MapField.PROJECTILE_SPEED).floatValue();

        builder.shootSpeed *= ctx.calculatedSpellData.data.getNumber(EventData.PROJECTILE_SPEED_MULTI, 1).number;

        builder.apart = data.getOrDefault(MapField.PROJECTILES_APART, 75D)
                .floatValue();

        if (posSource == PositionSource.SOURCE_ENTITY) {
            builder.pitch = ctx.sourceEntity.getXRot();
            builder.yaw = ctx.sourceEntity.getYRot();
        }
        if (shootWay == ShootWay.DOWN) {
            builder.fallDown = true;
        }
        if (shootWay == ShootWay.FIND_ENEMY) {
            builder.targetEnemy = true;
        }

        builder.randomSpreadDegrees = data.getOrDefault(MapField.PROJECTILE_SPREAD_RANDOMNESS, 0D).floatValue();

        builder.randomSpreadDegrees *= ctx.calculatedSpellData.data.getNumber(EventData.PROJECTILE_SPREAD_RANDOMNESS, 1).number;

        if (data.has(MapField.MIN_PITCH)) {
            builder.pitch = Math.max(builder.pitch, data.get(MapField.MIN_PITCH).floatValue());
        }
        if (data.has(MapField.MAX_PITCH)) {
            builder.pitch = Math.min(builder.pitch, data.get(MapField.MAX_PITCH).floatValue());
        }
        if (data.has(MapField.PITCH)) {
            builder.pitch = data.get(MapField.PITCH).floatValue();
        }

        builder.pitchOffset = data.getOrDefault(MapField.PITCH_OFFSET, 0D).floatValue();

        builder.yawOffset = data.getOrDefault(MapField.YAW_OFFSET, 0D).floatValue();

        builder.offsetX = data.getOrDefault(MapField.X_OFFSET, 0D);
        builder.offsetY = data.getOrDefault(MapField.Y_OFFSET, 0D);
        builder.offsetZ = data.getOrDefault(MapField.Z_OFFSET, 0D);

        builder.lifespanTicks = data.get(MapField.LIFESPAN_TICKS).intValue();

        builder.cast();
    }

    // Main create method with spreadRandomness parameter
    public MapHolder create(Item item, Double projCount, Double speed, EntityType type, Double lifespan, boolean gravity, Double spreadRandomness) {
        MapHolder c = new MapHolder();
        c.put(MapField.PROJECTILE_COUNT, projCount);
        c.put(MapField.ENTITY_NAME, Spell.DEFAULT_EN_NAME);
        c.put(MapField.PROJECTILE_SPEED, speed);
        c.put(MapField.LIFESPAN_TICKS, lifespan);
        c.put(MapField.ITEM, VanillaUTIL.REGISTRY.items().getKey(item)
                .toString());
        c.put(MapField.GRAVITY, gravity);
        c.put(MapField.PROJECTILE_ENTITY, EntityType.getKey(type)
                .toString());
        c.put(MapField.PROJECTILE_SPREAD_RANDOMNESS, spreadRandomness);
        c.type = GUID();
        return c;
    }

    public MapHolder create(Item item, Double projCount, Double speed, EntityType type, Double lifespan, boolean gravity) {
        return create(item, projCount, speed, type, lifespan, gravity, 0D);
    }

    public MapHolder create(Item item, Double speed, EntityType type, Double lifespan, Double spreadRandomness) {
        return create(item, 1D, speed, type, lifespan, true, spreadRandomness);
    }

    public MapHolder create(Item item, Double speed, EntityType type, Double lifespan) {
        return create(item, 1D, speed, type, lifespan, true, 0D);
    }

    public MapHolder createArrow(Double projCount, Double spreadRandomness) {
        MapHolder c = createBase(projCount, 3D, 80D, true);
        c.put(MapField.PROJECTILE_ENTITY, EntityType.getKey(SlashEntities.SIMPLE_ARROW.get())
                .toString());
        c.put(MapField.PROJECTILE_SPREAD_RANDOMNESS, spreadRandomness);
        return c;
    }

    public MapHolder createArrow(Double projCount) {
        return createArrow(projCount, 0D);
    }

    public MapHolder createFallingArrow(Double speed, Double spreadRandomness) {
        MapHolder c = createBase(1D, speed, 60D, true);
        c.put(MapField.PROJECTILE_ENTITY, EntityType.getKey(SlashEntities.SIMPLE_ARROW.get())
                .toString());
        c.put(MapField.POS_SOURCE, PositionSource.SOURCE_ENTITY.name());
        c.put(MapField.SHOOT_DIRECTION, ShootWay.DOWN.name());
        c.put(MapField.PROJECTILE_SPREAD_RANDOMNESS, spreadRandomness);
        return c;
    }

    public MapHolder createFallingArrow(Double speed) {
        return createFallingArrow(speed, 0D);
    }

    public MapHolder createTrident(Double projCount, Double speed, Double lifespan, Double spreadRandomness) {
        MapHolder c = createBase(projCount, speed, lifespan, true);
        c.put(MapField.PROJECTILE_ENTITY, EntityType.getKey(SlashEntities.SIMPLE_TRIDENT.get())
                .toString());
        c.put(MapField.PROJECTILE_SPREAD_RANDOMNESS, spreadRandomness);
        return c;
    }

    public MapHolder createTrident(Double projCount, Double speed, Double lifespan) {
        return createTrident(projCount, speed, lifespan, 0D);
    }

    private MapHolder createBase(Double projCount, Double speed, Double lifespan, boolean gravity) {
        MapHolder c = new MapHolder();
        c.put(MapField.PROJECTILE_COUNT, projCount);
        c.put(MapField.ENTITY_NAME, Spell.DEFAULT_EN_NAME);
        c.put(MapField.PROJECTILE_SPEED, speed);
        c.put(MapField.LIFESPAN_TICKS, lifespan);
        c.put(MapField.ITEM, VanillaUTIL.REGISTRY.items().getKey(Items.AIR)
                .toString());
        c.put(MapField.GRAVITY, gravity);
        c.type = GUID();
        return c;
    }

    @Override
    public String GUID() {
        return "projectile";
    }

}
