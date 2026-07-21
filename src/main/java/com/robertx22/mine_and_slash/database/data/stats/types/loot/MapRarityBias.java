package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class MapRarityBias extends Stat {

    private MapRarityBias() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.AQUA.getName();
    }

    public static MapRarityBias getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public boolean IsPercent() {
        return true;
    }

    @Override
    public Elements getElement() {
        return null;
    }

    @Override
    public String locDescForLangFile() {
        return "Increases the chance for dropped Maps to be of a higher rarity.";
    }

    @Override
    public String GUID() {
        return "map_rarity_bias";
    }

    @Override
    public String locNameForLangFile() {
        return "Map Rarity";
    }

    private static class SingletonHolder {
        private static final MapRarityBias INSTANCE = new MapRarityBias();
    }
}
