package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod;

import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;

public abstract class GearModification extends ItemModification {

    public GearModification(String serializer, String id) {
        super(serializer, id);
    }

    public abstract void modifyGear(ExileStack stack, ItemModificationResult r);

    @Override
    public void applyINTERNAL(ExileStack stack, ItemModificationResult r) {
        var gear = stack.get(StackKeys.GEAR).get();

        if (gear != null) {
            modifyGear(stack, r);
        }
    }

}
