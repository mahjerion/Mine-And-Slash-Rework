package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ProphecyDoubleCurse extends Stat {

    private ProphecyDoubleCurse() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static ProphecyDoubleCurse getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public boolean IsPercent() {
        return false;
    }

    @Override
    public Elements getElement() {
        return null;
    }

    @Override
    public String locDescForLangFile() {
        return "Prophecy Altars require you to take 2 curses instead of 1.";
    }

    @Override
    public String GUID() {
        return "prophecy_double_curse";
    }

    @Override
    public String locNameForLangFile() {
        return "Prophecy Double Curse";
    }

    private static class SingletonHolder {
        private static final ProphecyDoubleCurse INSTANCE = new ProphecyDoubleCurse();
    }
}
