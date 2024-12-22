package com.robertx22.mine_and_slash.database.data.currency.reworked.item_req;

import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import net.minecraft.world.entity.player.Player;

public abstract class JewelRequirement extends ItemRequirement {
    public JewelRequirement(String serializer, String id) {
        super(serializer, id);
    }

    public abstract boolean isJewelValid(ExileStack data);

    @Override
    public boolean isValid(Player p, ExileStack obj) {
        var data = obj.get(StackKeys.JEWEL).get();
        if (data != null) {
            return isJewelValid(obj);
        }
        return false;
    }
}
