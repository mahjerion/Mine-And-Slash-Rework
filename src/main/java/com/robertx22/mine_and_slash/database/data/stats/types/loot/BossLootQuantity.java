package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class BossLootQuantity extends Stat {

    private BossLootQuantity() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GOLD.getName();
    }

    public static BossLootQuantity getInstance() {
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
        return "Increases the amount of loot dropped by Map Bosses.";
    }

    @Override
    public String GUID() {
        return "boss_loot_quantity";
    }

    @Override
    public String locNameForLangFile() {
        return "Boss Loot Quantity";
    }

    private static class SingletonHolder {
        private static final BossLootQuantity INSTANCE = new BossLootQuantity();
    }
}
