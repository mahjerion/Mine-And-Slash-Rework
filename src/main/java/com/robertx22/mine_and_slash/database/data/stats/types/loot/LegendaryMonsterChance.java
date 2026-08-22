package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class LegendaryMonsterChance extends Stat {

    private LegendaryMonsterChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GOLD.getName();
    }

    public static LegendaryMonsterChance getInstance() {
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
        return "Increases the chance for monsters in Maps to spawn as Legendary.";
    }

    @Override
    public String GUID() {
        return "legendary_monster_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Increased Legendary Monster Chance";
    }

    private static class SingletonHolder {
        private static final LegendaryMonsterChance INSTANCE = new LegendaryMonsterChance();
    }
}
