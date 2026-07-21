package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class StatRollQuality extends Stat {

    private StatRollQuality() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GREEN.getName();
    }

    public static StatRollQuality getInstance() {
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
        return "Increases the quality of stat (prefix/suffix only) rolls on dropped gear.";
    }

    @Override
    public String GUID() {
        return "stat_roll_quality";
    }

    @Override
    public String locNameForLangFile() {
        return "Stat Roll Quality";
    }

    private static class SingletonHolder {
        private static final StatRollQuality INSTANCE = new StatRollQuality();
    }
}
