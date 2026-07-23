package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class HarvestCompletionBounty extends Stat {

    private HarvestCompletionBounty() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static HarvestCompletionBounty getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public boolean IsPercent() {
        return false;
    }

    @Override
    public Elements getElement() {
        return null;
    }

    @Override
    public String locDescForLangFile() {
        return "Successfully completing a Harvest grants you a temporary Currency Find buff.";
    }

    @Override
    public String GUID() {
        return "harvest_completion_bounty";
    }

    @Override
    public String locNameForLangFile() {
        return "Harvest Completion Bounty";
    }

    private static class SingletonHolder {
        private static final HarvestCompletionBounty INSTANCE = new HarvestCompletionBounty();
    }
}
