package com.robertx22.mine_and_slash.loot.generators;

import com.robertx22.library_of_exile.registry.FilterListWrap;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.gems.Gem;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;
import com.robertx22.mine_and_slash.vanilla_mc.items.gemrunes.GemItem;
import net.minecraft.world.item.ItemStack;

public class GemLootGen extends BaseLootGen<GearBlueprint> {

    public GemLootGen(LootInfo info) {
        super(info);
    }

    @Override
    public float baseDropChance() {
        return (float) (ServerContainer.get().GEM_DROPRATE.get().floatValue());
    }

    @Override
    public LootType lootType() {
        return LootType.Gem;
    }

    @Override
    public boolean condition() {
        return !droppableAtLevel(info.level).list.isEmpty();
    }

    @Override
    public ItemStack generateOne() {
        return droppableAtLevel(this.info.level).random().getItem().getDefaultInstance();
    }

    // Pinnacle-tier gems are excluded here explicitly (not just left to their weight=0) - they
    // must only ever come from PinnacleGemLootGen, never the normal per-kill pool
    public static FilterListWrap<Gem> droppableAtLevel(int lvl) {
        return ExileDB.Gems().getFilterWrapped(x -> lvl >= x.getReqLevelToDrop() && x.tier < GemItem.GemRank.PINNACLE.tier);
    }

}
