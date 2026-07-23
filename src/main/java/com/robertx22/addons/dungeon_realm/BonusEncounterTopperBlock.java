package com.robertx22.addons.dungeon_realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

// A purely-decorative block placed directly above one of the bonus-map-encounter blocks (Strongbox,
// Imprisoned Monster, Shrine) to make them 2-tall and more noticeable while exploring. Holds no state
// of its own - it survives only as long as the base block below it exists, and clears itself the
// instant that block is removed (unlock()/reward()/ShrineBlock.use() all need no changes for this).
public class BonusEncounterTopperBlock extends Block {

    private final Supplier<Block> baseBlock;

    public BonusEncounterTopperBlock(Supplier<Block> baseBlock, BlockBehaviour.Properties properties) {
        super(properties);
        this.baseBlock = baseBlock;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(baseBlock.get());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        // forwards straight to the base block's own use() - relies on it ignoring hit's position
        // (true for Strongbox/ImprisonedMonster/Shrine today), so clicking the top half of the
        // pillar still triggers the encounter.
        if (belowState.is(baseBlock.get())) {
            return belowState.getBlock().use(belowState, level, below, p, hand, hit);
        }
        return InteractionResult.PASS;
    }
}
