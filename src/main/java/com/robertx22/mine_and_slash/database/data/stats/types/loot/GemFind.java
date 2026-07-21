package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class GemFind extends Stat {

    private GemFind() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static GemFind getInstance() {
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
        return "Increases chance to find Gems (the socketable kind).";
    }

    @Override
    public String GUID() {
        return "gem_find";
    }

    @Override
    public String locNameForLangFile() {
        return "Gem Find";
    }

    private static class SingletonHolder {
        private static final GemFind INSTANCE = new GemFind();
    }
}
