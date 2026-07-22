package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ObeliskExtraDrops extends Stat {

    private ObeliskExtraDrops() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.GOLD.getName();
    }

    public static ObeliskExtraDrops getInstance() {
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
        return "Increases the amount of reward chests spawned by Ancient Obelisks.";
    }

    @Override
    public String GUID() {
        return "obelisk_extra_drops";
    }

    @Override
    public String locNameForLangFile() {
        return "Obelisk Extra Drops";
    }

    private static class SingletonHolder {
        private static final ObeliskExtraDrops INSTANCE = new ObeliskExtraDrops();
    }
}
