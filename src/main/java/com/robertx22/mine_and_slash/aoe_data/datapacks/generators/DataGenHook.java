package com.robertx22.mine_and_slash.aoe_data.datapacks.generators;

import com.robertx22.library_of_exile.recipe.RecipeGenerator;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.util.concurrent.CompletableFuture;

public class DataGenHook implements DataProvider {

    public DataGenHook() {

    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {


        new LootTableGenerator().generateAll(pOutput);

        MnsRecipeGenerator.addRecipes();

        new RecipeGenerator().generateAll(pOutput, SlashRef.MODID);

        for (ExileRegistryType type : ExileRegistryType.getAllInRegisterOrder()) {
            type.getDatapackGenerator().run(pOutput);
        }
        //DataProvider.saveStable(pOutput, x.serializeRecipe(), target);

        return CompletableFuture.completedFuture(null); // todo this is bad, but would it work?
        // i think this is only needed if you dont directly save the jsons yourself?
    }


    @Override
    public String getName() {
        return "hookdata";
    }
}
