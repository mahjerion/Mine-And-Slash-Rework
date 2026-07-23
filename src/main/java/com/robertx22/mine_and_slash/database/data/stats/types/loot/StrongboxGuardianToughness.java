package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class StrongboxGuardianToughness extends Stat {

    private StrongboxGuardianToughness() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GOLD.getName();
    }

    public static StrongboxGuardianToughness getInstance() {
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
        return "Increases the health and damage of Strongbox guardians.";
    }

    @Override
    public String GUID() {
        return "strongbox_guardian_toughness";
    }

    @Override
    public String locNameForLangFile() {
        return "Strongbox Guardian Strength";
    }

    private static class SingletonHolder {
        private static final StrongboxGuardianToughness INSTANCE = new StrongboxGuardianToughness();
    }
}
