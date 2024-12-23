package com.robertx22.mine_and_slash.uncommon.item_filters;

import com.robertx22.addons.orbs_of_crafting.currency.IItemAsCurrency;
import com.robertx22.mine_and_slash.uncommon.item_filters.bases.ItemFilter;
import net.minecraft.world.item.ItemStack;

public class CurrencyItemEffectFilter extends ItemFilter {

    @Override
    public boolean IsValidItem(ItemStack stack) {
        return stack.getItem() instanceof IItemAsCurrency;
    }
}

