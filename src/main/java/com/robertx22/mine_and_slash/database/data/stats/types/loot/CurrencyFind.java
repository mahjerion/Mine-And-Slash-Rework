package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class CurrencyFind extends Stat {

    private CurrencyFind() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static CurrencyFind getInstance() {
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
        return "Increases chance to find currency.";
    }

    @Override
    public String GUID() {
        return "currency_find";
    }

    @Override
    public String locNameForLangFile() {
        return "Currency Find";
    }

    private static class SingletonHolder {
        private static final CurrencyFind INSTANCE = new CurrencyFind();
    }
}
