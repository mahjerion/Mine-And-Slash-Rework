package com.robertx22.addons.orbs_of_crafting.currency.reworked;

import com.robertx22.addons.orbs_of_crafting.currency.base.CodeCurrency;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.addon.ExtendedOrb;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemMods;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqs;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.RarityKeyInfo;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.SkillItemTierKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolderSection;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyMap;
import com.robertx22.library_of_exile.registry.helpers.IdKey;
import com.robertx22.mine_and_slash.database.data.league.LeagueMechanics;
import com.robertx22.mine_and_slash.loot.req.DropRequirement;
import com.robertx22.mine_and_slash.mechanics.harvest.HarvestItems;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.RarityItems;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.orbs_of_crafting.main.OrbDatabase;
import com.robertx22.orbs_of_crafting.misc.ShapedRecipeUTIL;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import com.robertx22.orbs_of_crafting.register.Modifications;
import com.robertx22.temp.SkillItemTier;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;


public class HarvestCurrencies extends ExileKeyHolderSection<ExileCurrencies> {

    public HarvestCurrencies(ExileCurrencies holder) {
        super(holder);
    }

    public static void harvestOnlyDrop(String id) {
        new ExtendedOrb(id, DropRequirement.Builder.of().setOnlyDropsInLeague(LeagueMechanics.HARVEST_ID).build()).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
    }

    public ExileKeyMap<ExileCurrency, SkillItemTierKey> HARVEST_ESSENCE = new ExileKeyMap<ExileCurrency, SkillItemTierKey>(get(), "harvest_essence")
            .ofList(Arrays.stream(SkillItemTier.values()).toList().stream().filter(x -> x != SkillItemTier.TIER5).map(x -> new SkillItemTierKey(x)).collect(Collectors.toList()))
            .build((id, info) -> {

                harvestOnlyDrop(id);

                return ExileCurrency.Builder.of(id, "Harvested " + info.tier.word + " Essence", ItemReqs.INSTANCE.IS_GEAR)
                        .rarity(info.tier.rar)
                        .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
                        .addRequirement(ItemReqs.INSTANCE.NOT_CRAFTED)
                        .addRequirement(ItemReqs.INSTANCE.HAS_AFFIX_OF_RARITY.get(new RarityKeyInfo(info.tier.rar)))
                        .addModification(Modifications.INSTANCE.DESTROY_ITEM, 15)
                        .addModification(ItemMods.INSTANCE.UPGRADE_SPECIFIC_AFFIX_RARITY.get(new RarityKeyInfo(info.tier.rar)), 85)
                        .potentialCost(1)
                        .weight(5)
                        .buildCurrency(get());
            });

    public ExileKey<ExileCurrency, IdKey> HARVEST_AFFIX_UPGRADE = ExileCurrency.Builder.of("entangled_affix_upgrade", "Entangled Orb of Upgrade", ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.LEGENDARY_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.HAS_AFFIXES)
            .addModification(ItemMods.INSTANCE.UPGRADE_LOWEST_AFFIX, 50)
            .addModification(ItemMods.INSTANCE.CORRUPT_GEAR_NO_AFFIXES, 50)
            .potentialCost(1)
            .weight(CodeCurrency.Weights.UBER)
            .build(get());


    public ExileKey<ExileCurrency, IdKey> HARVEST_POTENTIAL_UPGRADE = ExileCurrency.Builder.of("entangled_potential", "Entangled Orb of Potential", ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.LEGENDARY_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addModification(ItemMods.INSTANCE.ADD_25_POTENTIAL, 75)
            .addModification(ItemMods.INSTANCE.CORRUPT_GEAR_NO_AFFIXES, 25)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.UBER)
            .build(get());


    public ExileKey<ExileCurrency, IdKey> HARVEST_QUALITY = ExileCurrency.Builder.of("entangled_quality", "Entangled Orb of Quality", ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.LEGENDARY_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.UNDER_21_QUALITY)
            .addModification(ItemMods.INSTANCE.ADD_UP_TO_5_GEAR_QUALITY, 75)
            .addModification(ItemMods.INSTANCE.CORRUPT_GEAR_NO_AFFIXES, 25)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.UBER)
            .build(get());

    public ExileKey<ExileCurrency, IdKey> HARVEST_UNIQUE_STATS = ExileCurrency.Builder.of("entangled_unique_reroll", "Entangled Orb of Imperfection", ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.LEGENDARY_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_RARITY.get(new RarityKeyInfo(IRarity.UNIQUE_ID)))
            .addModification(ItemMods.INSTANCE.ADD_10_PERCENT_UNIQUE_STATS, 30)
            .addModification(ItemMods.INSTANCE.CORRUPT_GEAR_NO_AFFIXES, 70)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.UBER)
            .build(get());

    @Override
    public void init() {

        harvestOnlyDrop(HARVEST_UNIQUE_STATS.GUID());
        harvestOnlyDrop(HARVEST_QUALITY.GUID());
        harvestOnlyDrop(HARVEST_POTENTIAL_UPGRADE.GUID());
        harvestOnlyDrop(HARVEST_AFFIX_UPGRADE.GUID());


        for (Map.Entry<SkillItemTierKey, ExileKey<ExileCurrency, SkillItemTierKey>> en : HARVEST_ESSENCE.map.entrySet()) {
            en.getValue().addRecipe(OrbDatabase.CURRENCY, x -> {
                return ShapedRecipeUTIL.of(x.getItem(), 1)
                        .define('X', HARVEST_AFFIX_UPGRADE.getItem())
                        .define('Y', RarityItems.RARITY_STONE.get(x.info.tier.higherTier().rar).get())
                        .pattern("YYY")
                        .pattern("YXY")
                        .pattern("YYY");
            });
        }


        HARVEST_AFFIX_UPGRADE.addRecipe(OrbDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 1)
                    .define('X', Items.IRON_INGOT)
                    .define('Y', HarvestItems.BLUE_INGOT.get())
                    .pattern("YYY")
                    .pattern("YXY")
                    .pattern("YYY");
        });

        HARVEST_POTENTIAL_UPGRADE.addRecipe(OrbDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 1)
                    .define('X', Items.GOLD_INGOT)
                    .define('Y', HarvestItems.PURPLE_INGOT.get())
                    .pattern("YYY")
                    .pattern("YXY")
                    .pattern("YYY");
        });
        HARVEST_QUALITY.addRecipe(OrbDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 1)
                    .define('X', Items.DIAMOND)
                    .define('Y', HarvestItems.GREEN_INGOT.get())
                    .pattern("YYY")
                    .pattern("YXY")
                    .pattern("YYY");
        });

        HARVEST_UNIQUE_STATS.addRecipe(OrbDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 1)
                    .define('X', Items.DIAMOND_BLOCK)
                    .define('Y', HarvestItems.PURPLE_INGOT.get())
                    .pattern("YYY")
                    .pattern("YXY")
                    .pattern("YYY");
        });

    }
}
