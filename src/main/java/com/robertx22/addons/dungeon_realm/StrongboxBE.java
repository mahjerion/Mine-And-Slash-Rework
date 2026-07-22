package com.robertx22.addons.dungeon_realm;

import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Block entity backing the locked Strongbox (see StrongboxBlock). Tracks how many guardians
// released when the box was opened are still alive; the block's ticker unlocks and rewards once
// the count hits zero. guardiansRemaining is decremented by a LivingDeathEvent hook (see
// DungeonAddonEvents) rather than polled by UUID, so it stays correct even if a guardian's chunk
// unloads while the box's own chunk stays loaded.
public class StrongboxBE extends BlockEntity {

    public boolean activated = false;
    public int guardiansRemaining = 0;
    public int tick = 0;

    public StrongboxBE(BlockPos pos, BlockState state) {
        super(SlashBlockEntities.STRONGBOX.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("activated", activated);
        tag.putInt("guardiansRemaining", guardiansRemaining);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        activated = tag.getBoolean("activated");
        guardiansRemaining = tag.getInt("guardiansRemaining");
    }
}
