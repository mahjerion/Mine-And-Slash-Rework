package com.robertx22.mine_and_slash.loot.generators;

import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.vanilla_mc.items.gemrunes.GemItem;
import net.minecraft.world.item.ItemStack;

// mirrors WatcherEyeLootGen's mob-rarity-gated shape, but for the Pinnacle gem tier instead of
// a Watcher Eye - this is the Pinnacle boss's own exclusive reward
public class PinnacleGemLootGen extends BaseLootGen<GearBlueprint> {

    public PinnacleGemLootGen(LootInfo info) {
        super(info);
    }

    @Override
    public float baseDropChance() {
        return (float) ServerContainer.get().PINNACLE_GEM_DROPRATE.get().floatValue();
    }

    @Override
    public boolean chanceIsModified() {
        return false;
    }

    @Override
    public LootType lootType() {
        return LootType.Gem;
    }

    @Override
    public boolean condition() {
        return info.mobData != null && info.mobData.getMobRarity().GUID().equals(IRarity.PINNACLE);
    }

    @Override
    public boolean hasLevelDistancePunishment() {
        return false;
    }

    @Override
    public ItemStack generateOne() {
        return ExileDB.Gems().getFilterWrapped(x -> x.tier == GemItem.GemRank.PINNACLE.tier).random().getItem().getDefaultInstance();
    }

}
