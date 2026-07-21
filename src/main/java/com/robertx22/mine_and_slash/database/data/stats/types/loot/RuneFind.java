package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class RuneFind extends Stat {

    private RuneFind() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static RuneFind getInstance() {
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
        return "Increases chance to find Runes.";
    }

    @Override
    public String GUID() {
        return "rune_find";
    }

    @Override
    public String locNameForLangFile() {
        return "Rune Find";
    }

    private static class SingletonHolder {
        private static final RuneFind INSTANCE = new RuneFind();
    }
}
