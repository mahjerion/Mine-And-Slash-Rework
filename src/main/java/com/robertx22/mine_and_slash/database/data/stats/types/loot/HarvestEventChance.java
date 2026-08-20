package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class HarvestEventChance extends Stat {

    private HarvestEventChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static HarvestEventChance getInstance() {
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
        return "Increases chance for a Harvest to appear in your Maps. Only applies if YOU start the Map.";
    }

    @Override
    public String GUID() {
        return "harvest_event_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Increased Harvest Event Chance";
    }

    private static class SingletonHolder {
        private static final HarvestEventChance INSTANCE = new HarvestEventChance();
    }
}
