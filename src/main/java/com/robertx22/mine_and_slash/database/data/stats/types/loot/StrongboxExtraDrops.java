package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class StrongboxExtraDrops extends Stat {

    private StrongboxExtraDrops() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GOLD.getName();
    }

    public static StrongboxExtraDrops getInstance() {
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
        return "Increases the amount of guaranteed category items a Strongbox pays out.";
    }

    @Override
    public String GUID() {
        return "strongbox_extra_drops";
    }

    @Override
    public String locNameForLangFile() {
        return "Strongbox Extra Drops";
    }

    private static class SingletonHolder {
        private static final StrongboxExtraDrops INSTANCE = new StrongboxExtraDrops();
    }
}
