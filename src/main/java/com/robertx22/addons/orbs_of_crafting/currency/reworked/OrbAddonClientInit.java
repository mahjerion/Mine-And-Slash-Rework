package com.robertx22.addons.orbs_of_crafting.currency.reworked;

import com.robertx22.library_of_exile.tooltip.register.ExileTooltipHooks;
import com.robertx22.orbs_of_crafting.api.CurrencyTooltip;

public class OrbAddonClientInit {

    public static void init() {

        ExileTooltipHooks.register(CurrencyTooltip.DEFAULT, new CurrencyTooltipHook());
    }
}
