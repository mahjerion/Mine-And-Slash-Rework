package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class OmenFind extends Stat {

    private OmenFind() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static OmenFind getInstance() {
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
        return "Increases chance to find Omens.";
    }

    @Override
    public String GUID() {
        return "omen_find";
    }

    @Override
    public String locNameForLangFile() {
        return "Omen Find";
    }

    private static class SingletonHolder {
        private static final OmenFind INSTANCE = new OmenFind();
    }
}
