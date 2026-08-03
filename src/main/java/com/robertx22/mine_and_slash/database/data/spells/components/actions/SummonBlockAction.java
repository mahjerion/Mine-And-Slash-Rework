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
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class SummonBlockAction extends SpellAction {

    public SummonBlockAction() {
        super(Arrays.asList(MapField.ENTITY_NAME, MapField.BLOCK));
    }

    static int SEARCH = 10;
    // same radius SummonPetAction uses to find a player's existing summons
    static double LIMIT_SEARCH_RADIUS = 100D;
    // minimum blocks kept between two blocks placed by the same cast
    static double RING_SPACING = 2D;


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

        Block block = data.getBlock();
        Objects.requireNonNull(block);

        BlockSummonLimitGroup group = BlockSummonLimitGroup.fromId(data.getOrDefault(MapField.SUMMON_LIMIT_GROUP, ""));

        int count = getSummonCount(ctx, group);

        Set<BlockPos> positions = findPositions(ctx, data, count);

        if (positions.isEmpty()) {
            return;
        }

        enforceSummonLimit(ctx, group, positions.size());

        for (BlockPos pos : positions) {
            StationaryFallingBlockEntity be = new StationaryFallingBlockEntity(ctx.world, pos, block.defaultBlockState());
            be.getEntityData().set(StationaryFallingBlockEntity.IS_FALLING, data.getOrDefault(MapField.IS_BLOCK_FALLING, false));
            SpellUtils.initSpellEntity(be, ctx.caster, ctx.calculatedSpellData, data);

            ctx.world.addFreshEntity(be);
        }
    }

    // one spot per block to place. a single block goes exactly where aimed, several get spread on a
    // ring around it. spots with no ground under them are dropped, so this can return less than count.
    static Set<BlockPos> findPositions(SpellCtx ctx, MapHolder data, int count) {

        //HitResult ray = ctx.caster.rayTrace(5D, 0.0F, false);
        MyPosition center = new MyPosition(ctx.getBlockPos());
        center = new MyPosition(center.x(), center.y() + data.getOrDefault(MapField.HEIGHT, 0D).intValue(), center.z());

        boolean snapToSurface = data.getOrDefault(MapField.FIND_NEAREST_SURFACE, true);

        Set<BlockPos> positions = new LinkedHashSet<>();

        for (int i = 0; i < count; i++) {

            MyPosition pos = count == 1 ? center : ringPos(center, i, count, ctx.caster.getYRot());

            float yoff = getRandomOffset(data, MapField.RANDOM_Y_OFFSET);
            float xoff = getRandomOffset(data, MapField.RANDOM_X_OFFSET);
            float zoff = getRandomOffset(data, MapField.RANDOM_Z_OFFSET);

            pos = new MyPosition(pos.x() + xoff, pos.y() + yoff, pos.z() + zoff);

            if (snapToSurface) {
                pos = findSurface(ctx.world, pos);
            }
            if (pos != null) {
                positions.add(pos.asBlockPos()); // a set, so terrain snapping can't stack two on one block
            }
        }
        return positions;
    }

    // how many blocks a single activation places. only limit groups can go above 1, and never above
    // the group's max, so +4 totems with a max of 3 still only places 3.
    static int getSummonCount(SpellCtx ctx, BlockSummonLimitGroup group) {
        if (group == null || !(ctx.caster instanceof Player)) {
            return 1;
        }
        int max = getMaxSummons(ctx, group);
        int extra = (int) ctx.calculatedSpellData.data.getNumber(group.extraCountEventDataKey, 0).number;

        return Math.max(1, Math.min(1 + extra, max));
    }

    static int getMaxSummons(SpellCtx ctx, BlockSummonLimitGroup group) {
        return Math.max(1, (int) ctx.calculatedSpellData.data.getNumber(group.maxEventDataKey, 0).number);
    }

    // spreads the blocks of one cast evenly on a circle around the target position, nothing in the
    // middle. the radius grows with the count so neighbours always stay ~RING_SPACING apart, and the
    // caster's yaw rotates the ring so it lines up with where they're looking.
    static MyPosition ringPos(MyPosition center, int index, int count, float casterYaw) {
        double radius = Math.max(RING_SPACING, (count * RING_SPACING) / (2 * Math.PI));
        double angle = Math.toRadians(casterYaw) + index * (2 * Math.PI / count);

        return new MyPosition(center.x() + Math.sin(angle) * radius, center.y(), center.z() + Math.cos(angle) * radius);
    }

    // walks down for open ground, then up if that failed. null when there's no spot to stand on.
    static MyPosition findSurface(Level level, MyPosition start) {

        MyPosition pos = start;
        int times = 0;

        while (pos.y() > level.getMinBuildHeight() && SEARCH > times) {
            times++;
            if (!isSolid(level, pos.asBlockPos()) && isSolid(level, pos.asBlockPos().below())) {
                return pos;
            }
            pos = new MyPosition(pos.x, pos.y - 1, pos.z);
        }

        pos = start;
        times = 0;

        while (pos.y() < level.getMaxBuildHeight() && SEARCH > times) {
            times++;
            if (!isSolid(level, pos.asBlockPos()) && isSolid(level, pos.asBlockPos().below())) {
                return pos;
            }
            pos = new MyPosition(pos.x, pos.y + 1, pos.z);
        }

        return null;
    }

    // removes the caster's oldest blocks of this limit group until there's room for `incoming` more.
    // called before the new blocks are spawned, so the cap is on the group as a whole, not per spell.
    static void enforceSummonLimit(SpellCtx ctx, BlockSummonLimitGroup group, int incoming) {

        if (group == null || !(ctx.caster instanceof Player)) {
            return;
        }

        int max = getMaxSummons(ctx, group);
        String casterUuid = ctx.caster.getStringUUID();

        List<StationaryFallingBlockEntity> existing = ctx.world.getEntitiesOfClass(
                StationaryFallingBlockEntity.class,
                ctx.caster.getBoundingBox().inflate(LIMIT_SEARCH_RADIUS),
                e -> group.id.equals(e.getLimitGroup()) && casterUuid.equals(e.getCasterUuid())
        );

        existing.sort(Comparator.comparingInt(e -> -e.tickCount)); // oldest first

        for (int i = 0; i < existing.size() - (max - incoming); i++) {
            existing.get(i).remove(Entity.RemovalReason.DISCARDED);
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

