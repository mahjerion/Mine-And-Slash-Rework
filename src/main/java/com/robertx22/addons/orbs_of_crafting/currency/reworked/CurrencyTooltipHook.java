package com.robertx22.addons.orbs_of_crafting.currency.reworked;

import com.robertx22.library_of_exile.tooltip.CurrencyTooltip;
import com.robertx22.library_of_exile.tooltip.TooltipBuilder;
import com.robertx22.library_of_exile.tooltip.order.ExileTooltipPart;
import com.robertx22.library_of_exile.tooltip.order.TooltipOrder;
import com.robertx22.library_of_exile.tooltip.register.ExileTooltipHook;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.texts.textblocks.RarityBlock;

public class CurrencyTooltipHook extends ExileTooltipHook<CurrencyTooltip> {

    @Override
    public void apply(TooltipBuilder<CurrencyTooltip> b) {

        b.add(x -> {
            return new ExileTooltipPart(TooltipOrder.LATE, new RarityBlock(ExileDB.GearRarities().get(b.item.currency.rar)).getAvailableComponents());
        });
        if (ExileDB.OrbExtension().isRegistered(b.item.currency.GUID())) {
            var req = ExileDB.OrbExtension().get(b.item.currency.GUID());
            if (req != null && req.drop_req.getLeague() != null) {
                b.add(x -> {
                    return new ExileTooltipPart(TooltipOrder.LATE, new RarityBlock(ExileDB.GearRarities().get(b.item.currency.rar)).getAvailableComponents());
                });
            }
        }

    }
}
