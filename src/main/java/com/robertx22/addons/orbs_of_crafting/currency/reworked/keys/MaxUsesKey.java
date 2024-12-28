package com.robertx22.addons.orbs_of_crafting.currency.reworked.keys;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.ExileCurrency;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemMods;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqs;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.custom.MaximumUsesReq;
import com.robertx22.orbs_of_crafting.keys.KeyInfo;

import java.util.function.Consumer;

public class MaxUsesKey extends KeyInfo {

    public MaximumUsesReq.Data data;

    public MaxUsesKey(MaximumUsesReq.Data data) {
        this.data = data;
    }

    @Override
    public String GUID() {
        return data.use_id();
    }

    public static Consumer<ExileCurrency.Builder> ofUses(MaxUsesKey key) {
        Consumer<ExileCurrency.Builder> c = x -> {
            x.useAllMods.add(new ExileCurrency.ItemModData(ItemMods.INSTANCE.MAXIMUM_USES.get(key).GUID(), 1));
            x.addRequirement(ItemReqs.INSTANCE.MAXIMUM_USES.get(key));
        };
        return c;
    }

}
