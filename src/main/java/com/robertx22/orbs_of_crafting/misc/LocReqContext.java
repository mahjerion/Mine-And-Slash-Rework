package com.robertx22.orbs_of_crafting.misc;

import com.robertx22.addons.orbs_of_crafting.currency.IItemAsCurrency;
import com.robertx22.addons.orbs_of_crafting.currency.base.CodeCurrency;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LocReqContext {

    public LocReqContext(Player player, ItemStack stack, ItemStack currency) {
        this.stack = stack;
        this.Currency = currency;
        this.player = player;

        if (currency.getItem() instanceof IItemAsCurrency cur) {
            effect = cur.currencyEffect(currency);
        }
    }

    public Player player;
    public ItemStack stack;
    public ItemStack Currency;
    public CodeCurrency effect;

    public boolean isValid() {
        return !stack.isEmpty() && !Currency.isEmpty();
    }

}
