package com.robertx22.mine_and_slash.database.data.spells.components.actions;

import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class SummonAtSightAction extends SpellAction {

    public SummonAtSightAction() {
        super(Arrays.asList(MapField.ENTITY_NAME, MapField.PROJECTILE_ENTITY, MapField.LIFESPAN_TICKS, MapField.HEIGHT));
    }


    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {

        if (ctx.world.isClientSide) {
            return;
        }

        Optional<EntityType<?>> projectile = EntityType.byString(data.get(MapField.PROJECTILE_ENTITY));

        Double distance = data.getOrDefault(MapField.DISTANCE, 10D);
        Double height = data.getOrDefault(MapField.HEIGHT, 10D);

        // the datapack may name the entity this lands on - the same MapField.POS_SOURCE that
        // SummonProjectileAction reads - otherwise the context's own source decides.
        PositionSource source = data.getOrDefault(ctx.getPositionSource());
        Entity posEn = source.get(ctx);
        if (posEn == null) {
            // onTick/onExpire leave target null when the source entity isn't a LivingEntity
            posEn = ctx.caster;
        }

        Vec3 pos;

        if (posEn == ctx.caster && posEn instanceof Player) {
            // someone is actually aiming, so trace from the crosshair - the whole point of "at sight"
            HitResult ray = posEn.pick(distance, 0.0F, false);
            pos = ray.getLocation();
        } else {
            // nobody is aiming here. a mob's pitch is wherever its LookControl last left it, and a
            // victim's facing has nothing to do with where the caster wants this - so drop it on the
            // entity itself rather than up to `distance` blocks off in a meaningless direction.
            pos = posEn.position();
        }

        Entity en = projectile.get()
                .create(ctx.world);
        SpellUtils.initSpellEntity(en, ctx.caster, ctx.calculatedSpellData, data);

        int yadd = 0;

        // must floor, not truncate toward zero - (int) -3.5 is -3, so the air scan below used to
        // read the wrong block everywhere in negative coordinates
        BlockPos bpos = BlockPos.containing(pos);


        for (int i = 0; i < height.intValue(); i++) {
            if (i == 0 || ctx.world.getBlockState(bpos.above(i)).isAir()) {
                yadd++;
            } else {
                break;
            }
        }

        en.setPos(pos.x, pos.y + yadd, pos.z);

        ctx.caster.level().addFreshEntity(en);

    }

    public MapHolder create(EntityType type, Double lifespan, Double height) {
        MapHolder c = new MapHolder();
        c.put(MapField.LIFESPAN_TICKS, lifespan);
        c.put(MapField.GRAVITY, false);
        c.put(MapField.ENTITY_NAME, Spell.DEFAULT_EN_NAME);
        c.put(MapField.HEIGHT, height);
        c.put(MapField.PROJECTILE_ENTITY, EntityType.getKey(type)
                .toString());
        c.type = GUID();
        this.validate(c);
        return c;
    }

    @Override
    public String GUID() {
        return "summon_at_sight";
    }

}
