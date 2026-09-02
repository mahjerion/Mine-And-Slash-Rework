package com.robertx22.mine_and_slash.database.data.spells.summons.entity.golems;

import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * The shared half of the three elemental golems: a full-element mob affix, and being rendered small.
 * <p>
 * What a golem CASTS is deliberately not here any more. It used to be an abstract {@code aoeSpell()}
 * returning one hard-coded spell id per subclass, which meant a datapack could neither retune it nor
 * add a second spell, and no other summon could have the behaviour at all. That list now lives on the
 * summon skill, as {@code SpellConfiguration.summon_spells}, and is fired by {@code SummonSpellCaster}.
 */
public abstract class GolemSummon extends SummonEntity {


    public GolemSummon(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public abstract String affix();

    public abstract Elements ele();

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {


        Load.Unit(this).getAffixData().affixes.add(affix());

        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public boolean isBaby() {
        return true;
    }
}
