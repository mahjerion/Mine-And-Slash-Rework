package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class EpicMonsterChance extends Stat {

    private EpicMonsterChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.LIGHT_PURPLE.getName();
    }

    public static EpicMonsterChance getInstance() {
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
        return "Increases the chance for monsters in Maps to spawn as Epic.";
    }

    @Override
    public String GUID() {
        return "epic_monster_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Epic Monster Chance";
    }

    private static class SingletonHolder {
        private static final EpicMonsterChance INSTANCE = new EpicMonsterChance();
    }
}
