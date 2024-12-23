package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req;

import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.world.entity.player.Player;

public abstract class GearRequirement extends ItemRequirement {
    public GearRequirement(String serializer, String id) {
        super(serializer, id);
    }

    public abstract boolean isGearValid(ExileStack gear);

    @Override
    public boolean isValid(Player p, ExileStack obj) {
        return obj.get(StackKeys.GEAR).has() && isGearValid(obj);
    }
}
