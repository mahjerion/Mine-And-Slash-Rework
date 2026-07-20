package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class UberFragmentFind extends Stat {

    private UberFragmentFind() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static UberFragmentFind getInstance() {
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
        return "Increases chance for map bosses to drop Uber Fragments";
    }

    @Override
    public String GUID() {
        return "uber_fragment_find";
    }

    @Override
    public String locNameForLangFile() {
        return "Uber Fragment Find";
    }

    private static class SingletonHolder {
        private static final UberFragmentFind INSTANCE = new UberFragmentFind();
    }
}
