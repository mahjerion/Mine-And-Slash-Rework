package com.robertx22.addons.orbs_of_crafting.currency.reworked.addon;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.util.ExplainedResult;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.orbs_of_crafting.api.OrbEvents;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;

public class OrbAddonEvents {


    public static void register() {
        OrbEvents.MODIFY.register(new EventConsumer<OrbEvents.Modify>() {
            @Override
            public void accept(OrbEvents.Modify event) {
                ExileStack ex = ExileStack.of(event.ctx.stack);

                var currency = event.currency;

                if (currency.potential.potential_cost > 0) {
                    StackKeys.POTENTIAL.get(ex).edit(x -> x.spend(currency.potential.potential_cost));
                    event.ctx.stack = ex.getStack();
                }
            }
        });

        OrbEvents.CAN_BE_MODIFIED.register(new EventConsumer<OrbEvents.CanBeModified>() {
            @Override
            public void accept(OrbEvents.CanBeModified event) {
                ExileStack ex = ExileStack.of(event.ctx.stack);

                if (!hasPotential(event.currency.potential, ex)) {
                    event.result = ExplainedResult.failure(Chats.GEAR_NO_POTENTIAL.locName());
                }
            }
        });
    }

    public static boolean hasPotential(ExileCurrency.PotentialData data, ExileStack stack) {
        if (!data.needs_potential) {
            return true;
        }
        if (data.potential_cost < 1) {
            return true;
        }
        return stack.get(StackKeys.POTENTIAL).hasAndTrue(x -> x.potential >= data.potential_cost);
    }
}
