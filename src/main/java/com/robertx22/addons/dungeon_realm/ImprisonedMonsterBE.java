package com.robertx22.addons.dungeon_realm;

import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Block entity backing the Imprisoned Monster encounter (see ImprisonedMonsterBlock). Tracks the
// caged elite(s) - normally just one, or two with the "Twin Captives" Atlas perk; the block's ticker
// rewards the killer once every released monster is dead.
//
// monstersRemaining is decremented by a LivingDeathEvent hook (see DungeonAddonEvents) rather than
// polled by UUID, matching StrongboxBE. Polling level.getEntity(uuid) treats "not currently loaded"
// as "dead", which paid out the guaranteed reward for free whenever a captive left the loaded area.
public class ImprisonedMonsterBE extends BlockEntity {

    public boolean activated = false;
    // how many captives were actually released - the reward payout scales off this, so it stays
    // correct for "Twin Captives" and stays 0 if the spawn failed outright
    public int spawnedCount = 0;
    public int monstersRemaining = 0;
    public int tick = 0;

    public ImprisonedMonsterBE(BlockPos pos, BlockState state) {
        super(SlashBlockEntities.IMPRISONED_MONSTER.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("activated", activated);
        tag.putInt("spawnedCount", spawnedCount);
        tag.putInt("monstersRemaining", monstersRemaining);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        activated = tag.getBoolean("activated");

        if (tag.contains("spawnedCount")) {
            spawnedCount = tag.getInt("spawnedCount");
            monstersRemaining = tag.getInt("monstersRemaining");
            return;
        }

        // legacy blocks saved before the switch to death-event counting stored a "monsters" UUID
        // list. Derive the counters from its size so an already-activated block in an existing world
        // neither soft-locks (remaining stuck at 0 with nothing to kill) nor pays out immediately.
        int legacy = tag.getList("monsters", 8).size();
        spawnedCount = legacy;
        monstersRemaining = legacy;
    }
}
