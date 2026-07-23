package com.robertx22.addons.dungeon_realm;

import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Block entity backing the Imprisoned Monster encounter (see ImprisonedMonsterBlock). Tracks the
// caged elite(s) - normally just one, or two with the "Twin Captives" Atlas perk; the block's ticker
// rewards the killer once every tracked monster is dead.
public class ImprisonedMonsterBE extends BlockEntity {

    public boolean activated = false;
    public List<UUID> monsters = new ArrayList<>();
    public int tick = 0;

    public ImprisonedMonsterBE(BlockPos pos, BlockState state) {
        super(SlashBlockEntities.IMPRISONED_MONSTER.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("activated", activated);
        ListTag list = new ListTag();
        for (UUID id : monsters) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put("monsters", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        activated = tag.getBoolean("activated");
        monsters = new ArrayList<>();
        for (var element : tag.getList("monsters", 8)) {
            monsters.add(UUID.fromString(element.getAsString()));
        }
    }
}
