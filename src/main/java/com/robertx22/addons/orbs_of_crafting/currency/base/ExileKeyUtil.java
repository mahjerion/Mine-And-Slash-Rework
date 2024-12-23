package com.robertx22.addons.orbs_of_crafting.currency.base;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.RarityKeyInfo;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.SkillItemTierKey;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.temp.SkillItemTier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExileKeyUtil {


    public static List<RarityKeyInfo> ofGearRarities() {
        return IRarity.ALL_GEAR_RARITIES.stream().map(x -> new RarityKeyInfo(x)).collect(Collectors.toList());
    }


    public static List<SkillItemTierKey> ofSkillItemTiers() {
        return Arrays.stream(SkillItemTier.values()).toList().stream().map(x -> new SkillItemTierKey(x)).collect(Collectors.toList());
    }
}
