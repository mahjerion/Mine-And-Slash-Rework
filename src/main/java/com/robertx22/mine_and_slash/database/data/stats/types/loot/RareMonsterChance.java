package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class RareMonsterChance extends Stat {

    private RareMonsterChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.AQUA.getName();
    }

    public static RareMonsterChance getInstance() {
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
        return "Increases the chance for monsters in Maps to spawn as Rare.";
    }

    @Override
    public String GUID() {
        return "rare_monster_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Rare Monster Chance";
    }

    private static class SingletonHolder {
        private static final RareMonsterChance INSTANCE = new RareMonsterChance();
    }
}
