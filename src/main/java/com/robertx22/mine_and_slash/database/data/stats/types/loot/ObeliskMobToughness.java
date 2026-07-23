package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ObeliskMobToughness extends Stat {

    private ObeliskMobToughness() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GOLD.getName();
    }

    public static ObeliskMobToughness getInstance() {
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
        return "Increases the health and damage of Obelisk monsters.";
    }

    @Override
    public String GUID() {
        return "obelisk_mob_toughness";
    }

    @Override
    public String locNameForLangFile() {
        return "Obelisk Mob Strength";
    }

    private static class SingletonHolder {
        private static final ObeliskMobToughness INSTANCE = new ObeliskMobToughness();
    }
}
