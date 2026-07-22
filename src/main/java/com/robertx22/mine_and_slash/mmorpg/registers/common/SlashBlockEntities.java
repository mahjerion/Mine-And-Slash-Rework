package com.robertx22.mine_and_slash.mmorpg.registers.common;

import com.robertx22.addons.dungeon_realm.ImprisonedMonsterBE;
import com.robertx22.addons.dungeon_realm.StrongboxBE;
import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.mine_and_slash.database.data.profession.ProfessionBlock;
import com.robertx22.mine_and_slash.database.data.profession.ProfessionBlockEntity;
import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.Def;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.stream.Collectors;

public class SlashBlockEntities {

    public static void init() {
    }


    public static RegObj<BlockEntityType<ProfessionBlockEntity>> PROFESSION = Def.blockEntity("profession", () -> {
        return BlockEntityType.Builder.of(ProfessionBlockEntity::new,
                SlashBlocks.STATIONS.values().stream().map(x -> x.get()).collect(Collectors.toList()).toArray(new ProfessionBlock[SlashBlocks.STATIONS.size()])
        ).build(null);
    });

    public static RegObj<BlockEntityType<StrongboxBE>> STRONGBOX = Def.blockEntity("strongbox", () ->
            BlockEntityType.Builder.of(StrongboxBE::new, SlashBlocks.STRONGBOX.get()).build(null));

    public static RegObj<BlockEntityType<ImprisonedMonsterBE>> IMPRISONED_MONSTER = Def.blockEntity("imprisoned_monster", () ->
            BlockEntityType.Builder.of(ImprisonedMonsterBE::new, SlashBlocks.IMPRISONED_MONSTER.get()).build(null));

}
