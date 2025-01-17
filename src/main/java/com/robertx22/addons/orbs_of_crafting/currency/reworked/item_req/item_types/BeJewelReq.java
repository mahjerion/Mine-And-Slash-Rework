package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.item_types;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import net.minecraft.world.entity.player.Player;

public class BeJewelReq extends BeItemTypeRequirement {
    public BeJewelReq() {
        super(ItemReqSers.IS_JEWEL, "is_jewel", "Must be a Jewel Item");
    }

    @Override
    public Class<?> getClassForSerialization() {
        return BeJewelReq.class;
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        return StackSaving.JEWEL.has(obj.stack);
    }
}