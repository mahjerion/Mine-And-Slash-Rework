package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class HarvestExtraDrops extends Stat {

    private HarvestExtraDrops() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GOLD.getName();
    }

    public static HarvestExtraDrops getInstance() {
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
        return "Increases the chance for Harvest mobs to drop bonus Harvest loot.";
    }

    @Override
    public String GUID() {
        return "harvest_extra_drops";
    }

    @Override
    public String locNameForLangFile() {
        return "Harvest Extra Drops";
    }

    private static class SingletonHolder {
        private static final HarvestExtraDrops INSTANCE = new HarvestExtraDrops();
    }
}
