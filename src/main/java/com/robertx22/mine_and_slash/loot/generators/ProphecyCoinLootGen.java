package com.robertx22.mine_and_slash.loot.generators;

import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ProphecyCoinFind;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.ItemBlueprint;
import com.robertx22.mine_and_slash.uncommon.coins.Coin;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;
import net.minecraft.world.item.ItemStack;

public class ProphecyCoinLootGen extends BaseLootGen<ItemBlueprint> {

    public ProphecyCoinLootGen(LootInfo info) {
        super(info);
    }

    @Override
    public float baseDropChance() {
        // runs from the BaseLootGen constructor, before condition() is ever asked. a player-less
        // LootInfo (spawner loot, give commands) used to NPE here, inside the one try/catch
        // MasterLootGen.populateOnce wraps every generator in, which silently skipped the omen
        // generator that runs after this one
        if (info.player == null) {
            return 0;
        }
        float chance = (float) ServerContainer.get().PROPHECY_COIN_DROPRATE.get().floatValue();
        chance *= 1F + (info.map_tier / 25F);
        chance *= Load.player(info.player).prophecy.affixesTaken.size();

        float coinFind = Load.Unit(info.player).getUnit().getCalculatedStat(ProphecyCoinFind.getInstance()).getValue();
        chance *= 1F + (coinFind / 100F);

        return chance;
    }

    @Override
    public boolean chanceIsModified() {
        return true;
    }

    @Override
    public LootType lootType() {
        return LootType.ProphecyCoin;
    }

    @Override
    public boolean condition() {
        // kills only. the chance scales per curse taken, a reward for fighting under them - it was
        // never meant for chests, and a chest carries the x10 MAP_CHEST and up to x3 completion
        // rarity multipliers on top (LootInfo.ofChestLoot / gatherLootMultipliers), which turned a
        // 67% per-kill chance into hundreds of coins in every reward room chest
        return info.lootOrigin == LootInfo.LootOrigin.MOB
                && info.isMapWorld && info.map != null && info.player != null
                && !Load.player(info.player).prophecy.affixesTaken.isEmpty();
    }

    @Override
    public boolean hasLevelDistancePunishment() {
        return true;
    }

    @Override
    public ItemStack generateOne() {
        ItemStack s = new ItemStack(Coin.PROPHECY.getItem());
        // LeveledItem.setTier(s, LevelUtils.levelToTier(info.level));
        return s;
    }

}