package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class PackSize extends Stat {

    private PackSize() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static PackSize getInstance() {
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
        return "Increases the amount of mobs that spawn in packs in maps.";
    }

    @Override
    public String GUID() {
        return "pack_size";
    }

    @Override
    public String locNameForLangFile() {
        return "Map Pack Size";
    }

    private static class SingletonHolder {
        private static final PackSize INSTANCE = new PackSize();
    }
}
