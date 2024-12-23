package com.robertx22.orbs_of_crafting.main;

import com.robertx22.mine_and_slash.database.data.profession.ExplainedResult;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;

public class ResultItem {

    public ExileStack stack;
    public ModifyResult resultEnum;
    public ExplainedResult result;

    public ItemModification.OutcomeType outcome = ItemModification.OutcomeType.GOOD;

    public ResultItem(ExileStack stack, ModifyResult resultEnum, ExplainedResult result) {
        this.stack = stack;
        this.resultEnum = resultEnum;
        this.result = result;
    }
}
