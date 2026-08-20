package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class StrongboxEventChance extends Stat {

    private StrongboxEventChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GOLD.getName();
    }

    public static StrongboxEventChance getInstance() {
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
        return "Increases the chance for a Strongbox to appear in your Maps. Only applies if YOU start the Map.";
    }

    @Override
    public String GUID() {
        return "strongbox_event_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Increased Strongbox Event Chance";
    }

    private static class SingletonHolder {
        private static final StrongboxEventChance INSTANCE = new StrongboxEventChance();
    }
}
