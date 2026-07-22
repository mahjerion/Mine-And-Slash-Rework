package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ImprisonedMonsterEventChance extends Stat {

    private ImprisonedMonsterEventChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.DARK_AQUA.getName();
    }

    public static ImprisonedMonsterEventChance getInstance() {
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
        return "Increases the chance for an Imprisoned Monster to appear in your Maps. Only applies if YOU start the Map.";
    }

    @Override
    public String GUID() {
        return "imprisoned_monster_event_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Imprisoned Monster Event Chance";
    }

    private static class SingletonHolder {
        private static final ImprisonedMonsterEventChance INSTANCE = new ImprisonedMonsterEventChance();
    }
}
