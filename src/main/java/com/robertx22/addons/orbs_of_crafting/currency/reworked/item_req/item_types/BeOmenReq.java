package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.item_types;

import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import net.minecraft.world.entity.player.Player;

public class BeOmenReq extends BeItemTypeRequirement {
    public BeOmenReq() {
        super("is_omen", "Must be an Omen Item");
    }

    @Override
    public Class<?> getClassForSerialization() {
        return BeOmenReq.class;
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        return StackSaving.OMEN.has(obj.stack);
    }
}