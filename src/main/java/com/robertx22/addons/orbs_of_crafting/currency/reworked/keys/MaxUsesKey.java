package com.robertx22.addons.orbs_of_crafting.currency.reworked.keys;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.custom.MaximumUsesReq;
import com.robertx22.orbs_of_crafting.keys.KeyInfo;

public class MaxUsesKey extends KeyInfo {

    public MaximumUsesReq.Data data;

    public MaxUsesKey(MaximumUsesReq.Data data) {
        this.data = data;
    }

    @Override
    public String GUID() {
        return data.use_id();
    }
}
