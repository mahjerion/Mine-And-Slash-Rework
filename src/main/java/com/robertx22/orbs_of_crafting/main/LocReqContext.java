package com.robertx22.orbs_of_crafting.main;

import com.robertx22.addons.orbs_of_crafting.currency.IItemAsCurrency;
import com.robertx22.addons.orbs_of_crafting.currency.base.CodeCurrency;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LocReqContext {

    public LocReqContext(Player player, ItemStack stack, ItemStack currency) {
        this.stack = ExileStack.of(stack);
        this.Currency = currency;
        this.player = player;

        if (currency.getItem() instanceof IItemAsCurrency cur) {
            effect = cur.currencyEffect(currency);
        }
    }

    public Player player;
    public ExileStack stack;
    public ItemStack Currency;
    public CodeCurrency effect;

    public boolean isValid() {
        return !stack.getStack().isEmpty() && !Currency.isEmpty();
    }

}
