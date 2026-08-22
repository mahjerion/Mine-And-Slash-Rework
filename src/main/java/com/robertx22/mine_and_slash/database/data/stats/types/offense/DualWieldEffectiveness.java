package com.robertx22.mine_and_slash.database.data.stats.types.offense;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class DualWieldEffectiveness extends Stat {

    public static String GUID = "dual_wield_effectiveness";

    private DualWieldEffectiveness() {
        this.group = StatGroup.WEAPON;
        this.is_perc = true;
        this.min = -100; // -100% zeroes the offhand out, anything below that would flip its stats
        this.format = ChatFormatting.GOLD.getName();
        this.icon = "\u2694";
    }

    public static DualWieldEffectiveness getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public Elements getElement() {
        return null;
    }

    @Override
    public String GUID() {
        return GUID;
    }

    @Override
    public String locNameForLangFile() {
        return "Increased Dual-Wield Effectiveness";
    }

    @Override
    public String locDescForLangFile() {
        return "Increases how much of an offhand weapon's stats you gain. An off-hand weapon grants 25% of its stats by default, so 10% Dual-Wield Effectiveness raises that to 27.5%.";
    }

    private static class SingletonHolder {
        private static final DualWieldEffectiveness INSTANCE = new DualWieldEffectiveness();
    }
}
