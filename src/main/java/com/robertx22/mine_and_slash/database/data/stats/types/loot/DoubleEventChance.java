package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class DoubleEventChance extends Stat {

    private DoubleEventChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static DoubleEventChance getInstance() {
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
        return "Chance for an additional bonus event to spawn in your Maps. Only applies if YOU start the Map.";
    }

    @Override
    public String GUID() {
        return "double_event_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Additional Event Chance";
    }

    private static class SingletonHolder {
        private static final DoubleEventChance INSTANCE = new DoubleEventChance();
    }
}
