package com.robertx22.library_of_exile.tooltip.register;

import com.robertx22.library_of_exile.tooltip.TooltipBuilder;
import com.robertx22.library_of_exile.tooltip.TooltipItem;

public abstract class ExileTooltipHook<T extends TooltipItem> {


    public abstract void apply(TooltipBuilder<T> b);

}
