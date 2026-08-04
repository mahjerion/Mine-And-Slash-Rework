package com.robertx22.addons.dungeon_realm;

import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

// Block entity backing the locked Strongbox (see StrongboxBlock). Tracks how many guardians
// released when the box was opened are still alive; the block's ticker unlocks and rewards once
// the count hits zero. guardiansRemaining is decremented by a LivingDeathEvent hook (see
// DungeonAddonEvents) rather than polled by UUID, so it stays correct even if a guardian's chunk
// unloads while the box's own chunk stays loaded.
public class StrongboxBE extends BlockEntity {

    public boolean activated = false;
    // how many guardians this box has ever released. Checked on top of `activated` before spawning:
    // it's the one guard that can't be defeated by a failure between the spawn loop and the box being
    // armed, which is what let a held right click pour out a fresh batch every few ticks.
    public int spawnedCount = 0;
    public int guardiansRemaining = 0;
    public int tick = 0;

    // who opened the box. Guardian toughness is rolled from this player's stats at spawn time, so
    // the payout resolves back to them first too - otherwise, in a party, the encounter could be
    // scaled by one player's Atlas stats and paid out using another's.
    @Nullable
    public UUID activatorId = null;

    public StrongboxBE(BlockPos pos, BlockState state) {
        super(SlashBlockEntities.STRONGBOX.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("activated", activated);
        tag.putInt("spawnedCount", spawnedCount);
        tag.putInt("guardiansRemaining", guardiansRemaining);
        if (activatorId != null) {
            tag.putUUID("activatorId", activatorId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        activated = tag.getBoolean("activated");
        // boxes saved before spawnedCount existed read back 0, which is fine: they were either never
        // opened, or already activated, and `activated` guards those on its own
        spawnedCount = tag.getInt("spawnedCount");
        guardiansRemaining = tag.getInt("guardiansRemaining");
        activatorId = tag.hasUUID("activatorId") ? tag.getUUID("activatorId") : null;
    }
}
