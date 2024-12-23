package com.robertx22.orbs_of_crafting.main;

import net.minecraft.advancements.critereon.EnchantedItemTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.level.ItemLike;

public class ShapedRecipeUTIL {
    public static ShapedRecipeBuilder of(ItemLike item, int count) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item, count)
                .unlockedBy("player_level", EnchantedItemTrigger.TriggerInstance.enchantedItem());

    }
}
