package com.robertx22.addons.orbs_of_crafting.currency.reworked.keys;

import com.robertx22.library_of_exile.registry.helpers.KeyInfo;

// todo..
public class RarityKeyInfo extends KeyInfo {

    public String rar;

    public RarityKeyInfo(String rar) {
        this.rar = rar;
    }

    @Override
    public String GUID() {
        return rar;
    }
}
