package com.robertx22.orbs_of_crafting.register.reqs;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.custom.MaximumUsesReq;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

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
    public boolean isValid(Player p, ExileStack stack) {
        return stack.getStack().getCount() == 1;
    }
}
