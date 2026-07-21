package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ObeliskEventChance extends Stat {

    private ObeliskEventChance() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static ObeliskEventChance getInstance() {
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
        return "Increases chance for an Obelisk to appear in your Maps. Only applies if YOU start the Map.";
    }

    @Override
    public String GUID() {
        return "obelisk_event_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Obelisk Event Chance";
    }

    private static class SingletonHolder {
        private static final ObeliskEventChance INSTANCE = new ObeliskEventChance();
    }
}
