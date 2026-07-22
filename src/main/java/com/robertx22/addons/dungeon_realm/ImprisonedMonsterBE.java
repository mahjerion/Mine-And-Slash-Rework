package com.robertx22.addons.dungeon_realm;

import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

// Block entity backing the Imprisoned Monster encounter (see ImprisonedMonsterBlock). Tracks the
// single caged elite; the block's ticker rewards the killer once it dies.
public class ImprisonedMonsterBE extends BlockEntity {

    public boolean activated = false;
    public UUID monster = null;
    public int tick = 0;

    public ImprisonedMonsterBE(BlockPos pos, BlockState state) {
        super(SlashBlockEntities.IMPRISONED_MONSTER.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("activated", activated);
        if (monster != null) {
            tag.putUUID("monster", monster);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        activated = tag.getBoolean("activated");
        monster = tag.hasUUID("monster") ? tag.getUUID("monster") : null;
    }
}
