package com.robertx22.orbs_of_crafting.register.reqs;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.orbs_of_crafting.main.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
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
    public boolean isValid(Player p, StackHolder obj) {
        return !obj.stack.isEnchanted();
    }

    @Override
    public String locDescForLangFile() {
        return "Must have no Enchantments";
    }
}
