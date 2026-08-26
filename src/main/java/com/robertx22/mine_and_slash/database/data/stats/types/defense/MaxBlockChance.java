package com.robertx22.mine_and_slash.database.data.stats.types.defense;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

/**
 * Raises the Block Chance cap, the way {@link com.robertx22.mine_and_slash.database.data.stats.effects.defense.MaxElementalResist}
 * raises the resistance cap. Read through {@link BlockChance#getAdditionalMax}.
 */
public class MaxBlockChance extends Stat {

    public static String GUID = "max_block_chance";

    public static MaxBlockChance getInstance() {
        return MaxBlockChance.SingletonHolder.INSTANCE;
    }

    private MaxBlockChance() {
        this.min = 0;
        // 75 base cap plus this is the 90 hard ceiling BlockChance declares
        this.max = 15;
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

    // there is no icon of its own, this is the same shield the Block Chance nodes use
    @Override
    public ResourceLocation getIconLocation() {
        return BlockChance.getInstance().getIconLocation();
    }

    @Override
    public String locNameForLangFile() {
        return "Maximum Block Chance";
    }

    @Override
    public String locDescForLangFile() {
        return "Raises the 75% Block Chance cap, up to a maximum of 90%.";
    }

    private static class SingletonHolder {
        private static final MaxBlockChance INSTANCE = new MaxBlockChance();
    }
}
