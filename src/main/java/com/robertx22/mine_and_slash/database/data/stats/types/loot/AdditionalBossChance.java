package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class AdditionalBossChance extends Stat {

    private AdditionalBossChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.RED.getName();
    }

    public static AdditionalBossChance getInstance() {
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
        return "Increases the chance for an additional Boss to spawn in your Maps.";
    }

    @Override
    public String GUID() {
        return "additional_boss_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Additional Boss Chance";
    }

    private static class SingletonHolder {
        private static final AdditionalBossChance INSTANCE = new AdditionalBossChance();
    }
}
