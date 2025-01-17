package com.robertx22.addons.orbs_of_crafting.currency.reworked.keys;

import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.temp.SkillItemTier;

public class SkillItemTierKey extends KeyInfo {

    public SkillItemTier tier;

    public SkillItemTierKey(SkillItemTier tier) {
        this.tier = tier;
    }

    @Override
    public String GUID() {
        return tier.tier + "";
    }
}
