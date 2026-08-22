package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class StrongboxUniqueChance extends Stat {

    private StrongboxUniqueChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GOLD.getName();
    }

    public static StrongboxUniqueChance getInstance() {
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
        return "Increases the chance a Strongbox's guaranteed category roll is a Unique item.";
    }

    @Override
    public String GUID() {
        return "strongbox_unique_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Increased Strongbox Unique Chance";
    }

    private static class SingletonHolder {
        private static final StrongboxUniqueChance INSTANCE = new StrongboxUniqueChance();
    }
}
