package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.item_types;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import net.minecraft.world.entity.player.Player;

public class BeGearReq extends BeItemTypeRequirement {
    public BeGearReq() {
        super(ItemReqSers.IS_GEAR, "is_gear", "Must be a Gear Item");
    }

    @Override
    public Class<?> getClassForSerialization() {
        return BeGearReq.class;
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        return StackSaving.GEARS.has(obj.stack);
    }
}
