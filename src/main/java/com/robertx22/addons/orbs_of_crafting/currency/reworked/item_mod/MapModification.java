package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod;

import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;

public abstract class MapModification extends ItemModification {

    public MapModification(String serializer, String id) {
        super(serializer, id);
    }

    public abstract void modifyMap(ExileStack map);

    @Override
    public void applyINTERNAL(ExileStack stack, ItemModificationResult r) {
        var map = stack.get(StackKeys.MAP).get();

        if (map != null) {
            modifyMap(stack);
        }
    }

}
