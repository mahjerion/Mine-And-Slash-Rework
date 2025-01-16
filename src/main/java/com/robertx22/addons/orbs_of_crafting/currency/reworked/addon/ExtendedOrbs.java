package com.robertx22.addons.orbs_of_crafting.currency.reworked.addon;

import com.robertx22.mine_and_slash.mmorpg.MMORPG;

public class ExtendedOrbs {


    public static String DEFAULT = "default";

    public static void init() {

        new ExtendedOrb().edit(x -> x.id = DEFAULT).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);

    }
}
