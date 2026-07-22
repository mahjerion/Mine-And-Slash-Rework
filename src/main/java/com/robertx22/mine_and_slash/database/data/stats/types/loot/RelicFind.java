package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class RelicFind extends Stat {

    private RelicFind() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static RelicFind getInstance() {
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
        return "Chance for boss relic drops to drop an extra relic, and for dungeon chests to contain a bonus relic.";
    }

    @Override
    public String GUID() {
        return "relic_find";
    }

    @Override
    public String locNameForLangFile() {
        return "Relic Find";
    }

    private static class SingletonHolder {
        private static final RelicFind INSTANCE = new RelicFind();
    }
}
