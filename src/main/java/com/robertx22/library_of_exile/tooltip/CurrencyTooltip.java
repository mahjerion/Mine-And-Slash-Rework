package com.robertx22.library_of_exile.tooltip;

import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import net.minecraft.world.item.ItemStack;

public class CurrencyTooltip extends TooltipItem {
    public static CurrencyTooltip DEFAULT = new CurrencyTooltip(ItemStack.EMPTY, null);


    public ItemStack stack;
    public ExileCurrency currency;


    public CurrencyTooltip(ItemStack stack, ExileCurrency currency) {
        super("currency");
        this.stack = stack;
        this.currency = currency;
    }
}
