package com.robertx22.mine_and_slash.database.data.currency.reworked.item_req.custom;

import com.robertx22.mine_and_slash.database.data.currency.reworked.item_req.ItemReqSers;
import com.robertx22.mine_and_slash.database.data.currency.reworked.item_req.ItemRequirement;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import net.minecraft.network.chat.MutableComponent;

public class IsSingleStack extends ItemRequirement {


    public MaximumUsesReq.Data data;


    public IsSingleStack(String id) {
        super(ItemReqSers.IS_SINGLE_ITEM, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsSingleStack.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getDescParams();
    }

    @Override
    public String locDescForLangFile() {
        return "Must be a Single Item";
    }

    @Override
    public boolean isValid(ExileStack stack) {
        return stack.getStack().getCount() == 1;
    }
}
