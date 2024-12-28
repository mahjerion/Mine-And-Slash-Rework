package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.GearRequirement;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class MustHaveAffixesReq extends GearRequirement {

    public MustHaveAffixesReq(String id) {
        super(ItemReqSers.HAS_AFFIXES, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return MustHaveAffixesReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getDescParams();
    }

    @Override
    public String locDescForLangFile() {
        return "Must have affixes";
    }

    @Override
    public boolean isGearValid(ItemStack stack) {
        ExileStack ex = ExileStack.of(stack);

        var gear = ex.get(StackKeys.GEAR).get();
        return gear.affixes != null && gear.affixes.getNumberOfAffixes() > 0;
    }
}
