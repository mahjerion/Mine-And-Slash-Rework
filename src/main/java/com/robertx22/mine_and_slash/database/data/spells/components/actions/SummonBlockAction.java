package com.robertx22.mine_and_slash.database.data.spells.components.actions;

import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.library_of_exile.utils.geometry.MyPosition;
import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.entities.StationaryFallingBlockEntity;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class SummonBlockAction extends SpellAction {

    public SummonBlockAction() {
        super(Arrays.asList(MapField.ENTITY_NAME, MapField.BLOCK));
    }

    static int SEARCH = 10;
    static double SAME_NAME_RADIUS = 10D;


    static boolean isSolid(Level level, BlockPos pos) {
        return level.getBlockState(pos).isSolid();
    }


    static float getRandomOffset(MapHolder data, MapField<Double> f) {
        float off = data.getOrDefault(f, 0D).floatValue();
        if (off != 0) {
            int te = (int) (off * 100F);
            float num = RandomUtils.RandomRange(0, te) / 100F;
            return RandomUtils.roll(50) ? num : -num;
        }
        return 0;
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {
        if (ctx.world.isClientSide) {
            return;
        }

        //HitResult ray = ctx.caster.rayTrace(5D, 0.0F, false);
        MyPosition pos = new MyPosition(ctx.getBlockPos());

        float yoff = getRandomOffset(data, MapField.RANDOM_Y_OFFSET);
        float xoff = getRandomOffset(data, MapField.RANDOM_X_OFFSET);
        float zoff = getRandomOffset(data, MapField.RANDOM_Z_OFFSET);


        pos = new MyPosition(pos.x() + xoff,
                pos.y() + data.getOrDefault(MapField.HEIGHT, 0D).intValue() + yoff,
                pos.z() + zoff);

        boolean found = true;


        if (data.getOrDefault(MapField.FIND_NEAREST_SURFACE, true)) {

            found = false;

            int times = 0;

            int minHeight = ctx.world.getMinBuildHeight();

            while (!found && pos.y() > minHeight && SEARCH > times) {
                times++;
                if (!isSolid(ctx.world, pos.asBlockPos()) && isSolid(ctx.world, pos.asBlockPos().below())) {
                    found = true;
                } else {
                    pos = new MyPosition(pos.x, pos.y - 1, pos.z);
                }
            }
            if (!found) {
                pos = new MyPosition(ctx.getBlockPos());
                times = 0;
                while (!found && pos.y() < ctx.world.getMaxBuildHeight() && SEARCH > times) {
                    times++;
                    if (!isSolid(ctx.world, pos.asBlockPos()) && isSolid(ctx.world, pos.asBlockPos().below())) {
                        found = true;
                    } else {
                        pos = new MyPosition(pos.x, pos.y + 1, pos.z);
                    }
                }
            }
        }
        Block block = data.getBlock();
        Objects.requireNonNull(block);


        if (found) {
            if (data.getOrDefault(MapField.DELETE_SAME_NAME, false)) {
                String entityName = data.getOrDefault(MapField.ENTITY_NAME, Spell.DEFAULT_EN_NAME);
                removeDuplicateNamedBlocks(ctx.world, pos, entityName);
            }

            StationaryFallingBlockEntity be = new StationaryFallingBlockEntity(ctx.world, pos.asBlockPos(), block.defaultBlockState());
            be.getEntityData().set(StationaryFallingBlockEntity.IS_FALLING, data.getOrDefault(MapField.IS_BLOCK_FALLING, false));
            SpellUtils.initSpellEntity(be, ctx.caster, ctx.calculatedSpellData, data);

            ctx.world.addFreshEntity(be);
        }


    }

    static void removeDuplicateNamedBlocks(Level level, MyPosition pos, String entityName) {
        if (entityName == null || entityName.isEmpty()) {
            return;
        }

        AABB area = new AABB(
                pos.x() - SAME_NAME_RADIUS, pos.y() - SAME_NAME_RADIUS, pos.z() - SAME_NAME_RADIUS,
                pos.x() + SAME_NAME_RADIUS, pos.y() + SAME_NAME_RADIUS, pos.z() + SAME_NAME_RADIUS
        );

        List<StationaryFallingBlockEntity> nearby = level.getEntitiesOfClass(
                StationaryFallingBlockEntity.class,
                area,
                e -> entityName.equals(e.getEntityName())
        );

        for (StationaryFallingBlockEntity old : nearby) {
            old.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    public MapHolder create(Block block, Double lifespan) {
        MapHolder c = new MapHolder();
        c.put(MapField.BLOCK, VanillaUTIL.REGISTRY.blocks().getKey(block)
                .toString());
        c.put(MapField.ENTITY_NAME, Spell.DEFAULT_EN_NAME);
        c.put(MapField.LIFESPAN_TICKS, lifespan);
        c.type = GUID();
        return c;
    }

    @Override
    public String GUID() {
        return "summon_block";
    }
}

