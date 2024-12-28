package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.GearRequirement;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class IsGearNotCraftedReq extends GearRequirement {

    public IsGearNotCraftedReq(String id) {
        super(ItemReqSers.NOT_CRAFTED_GEAR, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsGearNotCraftedReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getDescParams();
    }

    @Override
    public boolean isGearValid(ItemStack stack) {
        ExileStack ex = ExileStack.of(stack);

        return !ex.get(StackKeys.CUSTOM).getOrCreate().data.get(CustomItemData.KEYS.CRAFTED);
    }

    @Override
    public String locDescForLangFile() {
        return "Must not be a Crafted Item";
    }

}
