package com.robertx22.orbs_of_crafting.misc;

import com.robertx22.library_of_exile.util.ExplainedResult;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import net.minecraft.world.item.ItemStack;

public class ResultItem {

    public ItemStack stack;
    public ModifyResult resultEnum;
    public ExplainedResult result;

    public ItemModification.OutcomeType outcome = ItemModification.OutcomeType.GOOD;

    public ResultItem(ItemStack stack, ModifyResult resultEnum, ExplainedResult result) {
        this.stack = stack;
        this.resultEnum = resultEnum;
        this.result = result;
    }
}
