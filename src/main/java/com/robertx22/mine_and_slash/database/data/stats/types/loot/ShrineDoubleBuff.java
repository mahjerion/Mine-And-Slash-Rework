package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ShrineDoubleBuff extends Stat {

    private ShrineDoubleBuff() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static ShrineDoubleBuff getInstance() {
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
        return "Shrines grant you 2 random buffs at once, but each buff's duration is 25% shorter.";
    }

    @Override
    public String GUID() {
        return "shrine_double_buff";
    }

    @Override
    public String locNameForLangFile() {
        return "Shrine Double Buff";
    }

    private static class SingletonHolder {
        private static final ShrineDoubleBuff INSTANCE = new ShrineDoubleBuff();
    }
}
