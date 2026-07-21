package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class JewelFind extends Stat {

    private JewelFind() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static JewelFind getInstance() {
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
        return "Increases chance to find Jewels.";
    }

    @Override
    public String GUID() {
        return "jewel_find";
    }

    @Override
    public String locNameForLangFile() {
        return "Jewel Find";
    }

    private static class SingletonHolder {
        private static final JewelFind INSTANCE = new JewelFind();
    }
}
