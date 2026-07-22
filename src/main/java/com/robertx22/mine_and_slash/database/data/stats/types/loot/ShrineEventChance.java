package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ShrineEventChance extends Stat {

    private ShrineEventChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GOLD.getName();
    }

    public static ShrineEventChance getInstance() {
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
        return "Increases the chance for a Shrine to appear in your Maps. Only applies if YOU start the Map.";
    }

    @Override
    public String GUID() {
        return "shrine_event_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Shrine Event Chance";
    }

    private static class SingletonHolder {
        private static final ShrineEventChance INSTANCE = new ShrineEventChance();
    }
}
