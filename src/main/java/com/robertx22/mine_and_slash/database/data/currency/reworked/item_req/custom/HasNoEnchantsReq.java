package com.robertx22.mine_and_slash.database.data.currency.reworked.item_req.custom;

import com.robertx22.mine_and_slash.database.data.currency.reworked.item_req.ItemReqSers;
import com.robertx22.mine_and_slash.database.data.currency.reworked.item_req.ItemRequirement;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class HasNoEnchantsReq extends ItemRequirement {
    public HasNoEnchantsReq(String id) {
        super(ItemReqSers.HAS_NO_ENCHANTS, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return HasNoEnchantsReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getDescParams();
    }

    @Override
    public boolean isValid(Player p, ExileStack obj) {
        return !obj.getStack().isEnchanted();
    }

    @Override
    public String locDescForLangFile() {
        return "Must have no Enchantments";
    }
}
