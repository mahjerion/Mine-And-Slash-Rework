package com.robertx22.mine_and_slash.database.data.rarities;

import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.Rarity;

public interface IGearRarity<Self extends IGearRarity<Self>> extends Rarity<Self>, SalvagableItem {
  

    float valueMulti();


    float itemTierPower();

    int getAffixAmount();

    default int maximumOfOneAffixType() {
        return getAffixAmount() / 2;
    }


}
