package com.robertx22.mine_and_slash.database.data.currency.reworked.item_req.custom;

import com.robertx22.mine_and_slash.database.data.currency.reworked.item_req.ItemReqSers;
import com.robertx22.mine_and_slash.database.data.currency.reworked.item_req.ItemRequirement;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class IsDamagedReq extends ItemRequirement {

    public IsDamagedReq(String id) {
        super(ItemReqSers.IS_DAMAGED, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsDamagedReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getDescParams();
    }

    @Override
    public boolean isValid(Player p, ExileStack s) {
        return s.getStack().isDamaged();
    }

    @Override
    public String locDescForLangFile() {
        return "Must be a Damaged Item";
    }


}