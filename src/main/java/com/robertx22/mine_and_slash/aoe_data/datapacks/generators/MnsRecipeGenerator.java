package com.robertx22.mine_and_slash.aoe_data.datapacks.generators;

import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.library_of_exile.recipe.RecipeGenerator;
import com.robertx22.mine_and_slash.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.mine_and_slash.database.data.gear_slots.GearSlot;
import com.robertx22.mine_and_slash.database.data.profession.all.ProfessionMatItems;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.uncommon.IShapedRecipe;
import com.robertx22.mine_and_slash.uncommon.IShapelessRecipe;
import com.robertx22.mine_and_slash.vanilla_mc.items.gearitems.VanillaMaterial;
import joptsimple.internal.Strings;
import net.minecraft.advancements.critereon.EnchantedItemTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;

public class MnsRecipeGenerator {

    public static void addRecipes() {

        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof IShapedRecipe) {
                IShapedRecipe ir = (IShapedRecipe) item;
                RecipeGenerator.addRecipe(SlashRef.MODID, () -> ir.getRecipe());
            }
            if (item instanceof IShapelessRecipe) {
                IShapelessRecipe sr = (IShapelessRecipe) item;
                RecipeGenerator.addRecipe(SlashRef.MODID, () -> sr.getRecipe());
            }
        }


        gearRecipe(SlashItems.GearItems.NECKLACES, GearSlots.NECKLACE);
        gearRecipe(SlashItems.GearItems.RINGS, GearSlots.RING);
        gearRecipe(SlashItems.GearItems.STAFFS, GearSlots.STAFF);

        ProfessionMatItems.addDownRankRecipes();

    }

    static void gearRecipe(HashMap<VanillaMaterial, RegObj<Item>> map, String slot) {

        map.entrySet()
                .forEach(x -> {


                    RecipeGenerator.addRecipe(SlashRef.MODID, () -> {

                        ShapedRecipeBuilder fac = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, x.getValue()
                                .get(), 1);

                        String[] pattern = getRecipePattern(ExileDB.GearSlots()
                                .get(slot));

                        String all = Strings.join(pattern, "");

                        if (all.contains("M")) {
                            if (x.getKey().mat.tag != null) {
                                fac.define('M', x.getKey().mat.tag);
                            } else {
                                fac.define('M', x.getKey().mat.item);
                            }
                        }
                        if (all.contains("S")) {
                            fac.define('S', Items.STICK);
                        }
                        if (all.contains("B")) {
                            fac.define('B', Items.STRING);
                        }

                        for (String pat : pattern) {
                            try {
                                fac.pattern(pat);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        fac.unlockedBy("player_level", EnchantedItemTrigger.TriggerInstance.enchantedItem());

                        return fac;
                    });

                });
    }

    static String[] getRecipePattern(GearSlot type) {

        String id = type.id;

        if (id.equals(GearSlots.SWORD)) {
            return new String[]{
                    " M ",
                    " M ",
                    " S "
            };
        }


        if (id.equals(GearSlots.STAFF)) {
            return new String[]{
                    "  M",
                    "SM ",
                    "SS "
            };
        }

        if (id.equals(GearSlots.BOW)) {
            return new String[]{
                    " MB",
                    "M B",
                    " MB"
            };
        }
        if (id.equals(GearSlots.CROSBOW)) {
            return new String[]{
                    "MSM",
                    "S S",
                    " S "
            };
        }

        if (id.equals(GearSlots.CHEST)) {
            return new String[]{
                    "M M",
                    "MMM",
                    "MMM"
            };
        }
        if (id.equals(GearSlots.BOW)) {
            return new String[]{
                    "M M",
                    "M M"
            };
        }
        if (id.equals(GearSlots.PANTS)) {
            return new String[]{
                    "MMM",
                    "M M",
                    "M M"
            };
        }
        if (id.equals(GearSlots.HELMET)) {
            return new String[]{
                    "MMM",
                    "M M"
            };
        }

        if (id.equals(GearSlots.NECKLACE)) {
            return new String[]{
                    "MMM",
                    "M M",
                    "MMM"
            };
        }
        if (id.equals(GearSlots.RING)) {
            return new String[]{
                    " M ",
                    "M M",
                    " M "
            };
        }

        System.out.print("NO RECIPE FOR TAG ");

        return null;
    }


}
