package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.item_types;

import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import net.minecraft.world.entity.player.Player;

public class BeDungeonMapReq extends BeItemTypeRequirement {
    public BeDungeonMapReq(String id) {
        super(id, "Must be an Adventure Map Item");
    }

    @Override
    public Class<?> getClassForSerialization() {
        return BeDungeonMapReq.class;
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        return StackSaving.MAP.has(obj.stack);
    }
}
