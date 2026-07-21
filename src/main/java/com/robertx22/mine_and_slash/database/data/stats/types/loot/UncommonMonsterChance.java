package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class UncommonMonsterChance extends Stat {

    private UncommonMonsterChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GREEN.getName();
    }

    public static UncommonMonsterChance getInstance() {
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
        return "Increases the chance for monsters in Maps to spawn as Uncommon.";
    }

    @Override
    public String GUID() {
        return "uncommon_monster_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Uncommon Monster Chance";
    }

    private static class SingletonHolder {
        private static final UncommonMonsterChance INSTANCE = new UncommonMonsterChance();
    }
}
