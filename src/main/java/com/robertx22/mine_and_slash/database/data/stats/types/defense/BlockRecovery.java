package com.robertx22.mine_and_slash.database.data.stats.types.defense;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

public class BlockRecovery extends Stat {

    public static String GUID = "block_recovery";

    public static BlockRecovery getInstance() {
        return BlockRecovery.SingletonHolder.INSTANCE;
    }

    private BlockRecovery() {
        this.min = 0;
        this.max = 200;
        this.group = StatGroup.MAIN;

        this.format = ChatFormatting.BLUE.getName();
    }

    @Override
    public String GUID() {
        return GUID;
    }

    @Override
    public Elements getElement() {
        return Elements.Physical;
    }

    @Override
    public boolean IsPercent() {
        return true;
    }

    // there is no block_recovery icon of its own, this is the same shield the Block Chance nodes use
    @Override
    public ResourceLocation getIconLocation() {
        return BlockChance.getInstance().getIconLocation();
    }

    @Override
    public String locNameForLangFile() {
        return "Increased Block Recovery";
    }

    @Override
    public String locDescForLangFile() {
        return "How quickly you can block again. Blocking has a 1 second cooldown, and +100% means it recovers twice as fast.";
    }

    private static class SingletonHolder {
        private static final BlockRecovery INSTANCE = new BlockRecovery();
    }
}
