package com.robertx22.orbs_of_crafting.main;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.util.concurrent.CompletableFuture;

public class OrbDataGen implements DataProvider {

    public OrbDataGen() {

    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        OrbDatabase.ITEM_REQ.getDatapackGenerator().run(pOutput);
        OrbDatabase.ITEM_MOD.getDatapackGenerator().run(pOutput);
        OrbDatabase.CURRENCY.getDatapackGenerator().run(pOutput);


        return CompletableFuture.completedFuture(null); // todo this is bad, but would it work?

        /*
        ObeliskDatabase.OBELISK_TYPE.getDatapackGenerator().run(pOutput);
        ObeliskDatabase.ATTRIBUTE_AFFIX.getDatapackGenerator().run(pOutput);

        //DataProvider.saveStable(pOutput, x.serializeRecipe(), target);


         */
        // i think this is only needed if you dont directly save the jsons yourself?
    }


    @Override
    public String getName() {
        return "orbs_data";
    }
}
