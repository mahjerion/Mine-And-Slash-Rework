package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class WatcherEyeFind extends Stat {

    private WatcherEyeFind() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static WatcherEyeFind getInstance() {
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
        return "Increases chance for Uber bosses to drop Watcher Eyes.";
    }

    @Override
    public String GUID() {
        return "watcher_eye_find";
    }

    @Override
    public String locNameForLangFile() {
        return "Watcher Eye Find";
    }

    private static class SingletonHolder {
        private static final WatcherEyeFind INSTANCE = new WatcherEyeFind();
    }
}
