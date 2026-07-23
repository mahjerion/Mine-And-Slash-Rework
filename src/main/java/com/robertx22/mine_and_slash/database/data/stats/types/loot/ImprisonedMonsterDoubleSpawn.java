package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ImprisonedMonsterDoubleSpawn extends Stat {

    private ImprisonedMonsterDoubleSpawn() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static ImprisonedMonsterDoubleSpawn getInstance() {
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
        return "Imprisoned Monsters release 2 caged monsters instead of 1, for double the total reward.";
    }

    @Override
    public String GUID() {
        return "imprisoned_monster_double_spawn";
    }

    @Override
    public String locNameForLangFile() {
        return "Imprisoned Monster Double Spawn";
    }

    private static class SingletonHolder {
        private static final ImprisonedMonsterDoubleSpawn INSTANCE = new ImprisonedMonsterDoubleSpawn();
    }
}
