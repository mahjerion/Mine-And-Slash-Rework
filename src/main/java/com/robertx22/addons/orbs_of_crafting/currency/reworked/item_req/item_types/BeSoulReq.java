package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.item_types;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.mine_and_slash.database.data.profession.items.CraftedSoulItem;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import net.minecraft.world.entity.player.Player;

public class BeSoulReq extends BeItemTypeRequirement {
    public BeSoulReq() {
        super(ItemReqSers.IS_SOUL, "is_soul", "Must be a Soul Item");
    }

    @Override
    public Class<?> getClassForSerialization() {
        return BeSoulReq.class;
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        return StackSaving.STAT_SOULS.has(obj.stack) || obj.stack.getItem() instanceof CraftedSoulItem;
    }
}