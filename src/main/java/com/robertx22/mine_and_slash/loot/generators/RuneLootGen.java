package com.robertx22.mine_and_slash.loot.generators;

import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.RuneFind;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.RuneBlueprint;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;
import net.minecraft.world.item.ItemStack;

public class RuneLootGen extends BaseLootGen<GearBlueprint> {

    public RuneLootGen(LootInfo info) {
        super(info);

    }

    @Override
    public float baseDropChance() {
        float chance = (float) (ServerContainer.get().RUNE_DROPRATE.get().floatValue());

        if (info.player != null) {
            float runeFind = Load.Unit(info.player).getUnit().getCalculatedStat(RuneFind.getInstance()).getValue();
            chance *= 1F + (runeFind / 100F);
        }

        return chance;
    }

    @Override
    public LootType lootType() {
        return LootType.Rune;
    }

    @Override
    public boolean condition() {
        return this.info.level > 10;
    }

    @Override
    public ItemStack generateOne() {
        RuneBlueprint b = new RuneBlueprint(info);

        return b.createStack();

    }

}

