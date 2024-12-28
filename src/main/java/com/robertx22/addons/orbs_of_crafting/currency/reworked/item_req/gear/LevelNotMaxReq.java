package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.GearRequirement;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class LevelNotMaxReq extends GearRequirement {

    public LevelNotMaxReq(String id) {
        super(ItemReqSers.LVL_NOT_MAX, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return LevelNotMaxReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getDescParams(GameBalanceConfig.get().MAX_LEVEL);
    }

    @Override
    public String locDescForLangFile() {
        return "Level must be below %1$s";
    }

    @Override
    public boolean isGearValid(ItemStack stack) {
        ExileStack ex = ExileStack.of(stack);

        var gear = ex.get(StackKeys.GEAR).get();
        return gear.lvl < GameBalanceConfig.get().MAX_LEVEL;
    }
}
