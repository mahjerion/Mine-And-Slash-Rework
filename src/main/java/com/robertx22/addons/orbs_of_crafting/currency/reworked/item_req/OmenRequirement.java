package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req;

import com.robertx22.mine_and_slash.database.data.omen.OmenData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.world.entity.player.Player;

public abstract class OmenRequirement extends ItemRequirement {
    public OmenRequirement(String serializer, String id) {
        super(serializer, id);
    }

    public abstract boolean isOmenValid(OmenData omen);

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        ExileStack ex = ExileStack.of(obj.stack);

        return StackKeys.OMEN.get(ex).has() && isOmenValid(StackKeys.OMEN.get(ex).get());
    }
}
