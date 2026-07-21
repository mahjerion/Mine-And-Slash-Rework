package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class MythicMonsterChance extends Stat {

    private MythicMonsterChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.DARK_PURPLE.getName();
    }

    public static MythicMonsterChance getInstance() {
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
        return "Increases the chance for monsters in Maps to spawn as Mythic.";
    }

    @Override
    public String GUID() {
        return "mythic_monster_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Mythic Monster Chance";
    }

    private static class SingletonHolder {
        private static final MythicMonsterChance INSTANCE = new MythicMonsterChance();
    }
}
