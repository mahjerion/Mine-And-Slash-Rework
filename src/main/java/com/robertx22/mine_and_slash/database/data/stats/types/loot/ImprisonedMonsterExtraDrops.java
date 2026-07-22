package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ImprisonedMonsterExtraDrops extends Stat {

    private ImprisonedMonsterExtraDrops() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GOLD.getName();
    }

    public static ImprisonedMonsterExtraDrops getInstance() {
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
        return "Increases the amount of guaranteed currency an Imprisoned Monster pays out.";
    }

    @Override
    public String GUID() {
        return "imprisoned_monster_extra_drops";
    }

    @Override
    public String locNameForLangFile() {
        return "Imprisoned Monster Extra Drops";
    }

    private static class SingletonHolder {
        private static final ImprisonedMonsterExtraDrops INSTANCE = new ImprisonedMonsterExtraDrops();
    }
}
