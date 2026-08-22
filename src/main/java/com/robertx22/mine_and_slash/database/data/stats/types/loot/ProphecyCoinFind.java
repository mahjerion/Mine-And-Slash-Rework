package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ProphecyCoinFind extends Stat {

    private ProphecyCoinFind() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static ProphecyCoinFind getInstance() {
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
        return "Increases chance to find Prophecy Coins.";
    }

    @Override
    public String GUID() {
        return "prophecy_coin_find";
    }

    @Override
    public String locNameForLangFile() {
        return "Increased Prophecy Coin Find";
    }

    private static class SingletonHolder {
        private static final ProphecyCoinFind INSTANCE = new ProphecyCoinFind();
    }
}
