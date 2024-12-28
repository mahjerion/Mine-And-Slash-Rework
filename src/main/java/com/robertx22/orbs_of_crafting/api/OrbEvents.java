package com.robertx22.orbs_of_crafting.api;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.ExileCurrency;
import com.robertx22.library_of_exile.events.base.ExileEvent;
import com.robertx22.library_of_exile.events.base.ExileEventCaller;
import com.robertx22.mine_and_slash.database.data.profession.ExplainedResult;
import com.robertx22.orbs_of_crafting.main.LocReqContext;

public class OrbEvents {

    public static ExileEventCaller<CanBeModified> CAN_BE_MODIFIED = new ExileEventCaller<>();
    public static ExileEventCaller<Modify> MODIFY = new ExileEventCaller<>();

    public static class Modify extends ExileEvent {

        public ExileCurrency currency;

        public LocReqContext ctx;

        public Modify(ExileCurrency currency, LocReqContext ctx) {
            this.currency = currency;
            this.ctx = ctx;
        }
    }

    public static class CanBeModified extends ExileEvent {

        public ExplainedResult result = null;
        public ExileCurrency currency;

        public CanBeModified(ExileCurrency currency, LocReqContext ctx) {
            this.currency = currency;
            this.ctx = ctx;
        }

        public LocReqContext ctx;


    }

    public static void register() {

    }
}
