package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req;

import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class GearRequirement extends ItemRequirement {
    public GearRequirement(String serializer, String id) {
        super(serializer, id);
    }

    public abstract boolean isGearValid(ItemStack gear);

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        ExileStack ex = ExileStack.of(obj.stack);

        return StackKeys.GEAR.get(ex).has() && isGearValid(obj.stack);
    }
}
