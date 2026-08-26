package com.robertx22.mine_and_slash.database.data.stats.types.defense;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

public class BlockDamageReduction extends Stat {

    public static String GUID = "block_damage_reduction";

    public static BlockDamageReduction getInstance() {
        return BlockDamageReduction.SingletonHolder.INSTANCE;
    }

    private BlockDamageReduction() {
        this.min = 0;
        this.max = 100;
        // a block stops the whole hit unless something takes this down. read it with
        // StatData.getValueOrBase - base only lands in the calculation once some source touches the
        // stat, and a character with no such source has no entry at all
        this.base = 100;
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
        return "Block Damage Reduction";
    }

    @Override
    public String locDescForLangFile() {
        return "How much damage a successful block stops. At 100% a blocked hit does nothing at all.";
    }

    private static class SingletonHolder {
        private static final BlockDamageReduction INSTANCE = new BlockDamageReduction();
    }
}
