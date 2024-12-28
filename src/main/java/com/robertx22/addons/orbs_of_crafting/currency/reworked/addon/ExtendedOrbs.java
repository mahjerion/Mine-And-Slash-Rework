package com.robertx22.addons.orbs_of_crafting.currency.reworked.addon;

public class ExtendedOrbs {


    public static String DEFAULT = "default";

    public static void init() {

        new ExtendedOrb().edit(x -> x.id = DEFAULT).addToSerializables();

    }
}
