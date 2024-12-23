package com.robertx22.addons.orbs_of_crafting.currency;

import com.robertx22.addons.orbs_of_crafting.currency.base.CodeCurrency;
import net.minecraft.world.item.ItemStack;

public interface IItemAsCurrency {
    CodeCurrency currencyEffect(ItemStack stack);

}
