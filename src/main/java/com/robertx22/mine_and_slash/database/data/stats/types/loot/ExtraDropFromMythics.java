package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ExtraDropFromMythics extends Stat {

    private ExtraDropFromMythics() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.DARK_PURPLE.getName();
    }

    public static ExtraDropFromMythics getInstance() {
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
        return "Increases the amount of loot dropped by Mythic monsters.";
    }

    @Override
    public String GUID() {
        return "extra_drop_from_mythics";
    }

    @Override
    public String locNameForLangFile() {
        return "Mythic Extra Drops";
    }

    private static class SingletonHolder {
        private static final ExtraDropFromMythics INSTANCE = new ExtraDropFromMythics();
    }
}
