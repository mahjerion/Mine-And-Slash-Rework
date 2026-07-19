package com.robertx22.addons.orbs_of_crafting.currency.reworked;

import com.robertx22.addons.orbs_of_crafting.currency.base.CodeCurrency;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.addon.ExtendedOrb;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemMods;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqs;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.RarityKeyInfo;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.SkillItemTierKey;
import com.robertx22.ancient_obelisks.main.ObeliskEntries;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.library_of_exile.registry.helpers.*;
import com.robertx22.mine_and_slash.loot.req.DropRequirement;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.RarityItems;
import com.robertx22.mine_and_slash.tags.all.SlotTags;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.orbs_of_crafting.misc.ShapedRecipeUTIL;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import com.robertx22.orbs_of_crafting.register.Modifications;
import com.robertx22.temp.SkillItemTier;
import com.robertx22.the_harvest.main.HarvestEntries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;


// todo separate into new mod
public class AncientObeliskCurrencies extends ExileKeyHolderSection<ExileCurrencies> {

    public AncientObeliskCurrencies(ExileCurrencies holder) {
        super(holder);
    }

    public static void obeliskOnlyDrop(String id) {
        new ExtendedOrb(id, DropRequirement.Builder.of().setOnlyDropsInLeague(Ref.Obelisks.MODID).build()).addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
    }

    public ExileKeyMap<ExileCurrency, NamedKey> COMMON_TO_EPIC_WITH_TAG = new ExileKeyMap<ExileCurrency, NamedKey>(get(), "common_to_epic_with_tag")
            .ofList(Arrays.asList(
                    new NamedKey(SlotTags.intelligence.GUID(), "Intelligence"),
                    new NamedKey(SlotTags.strength.GUID(), "Strength"),
                    new NamedKey(SlotTags.dexterity.GUID(), "Dexterity")
            ))
            .build((id, info) -> {

                String currencyName;
                if (info.GUID().equals(SlotTags.intelligence.GUID())) {
                    currencyName = "Orb of the Aegis";
                } else if (info.GUID().equals(SlotTags.strength.GUID())) {
                    currencyName = "Orb of the Bulwark";
                } else if (info.GUID().equals(SlotTags.dexterity.GUID())) {
                    currencyName = "Orb of the Phantom";
                } else {
                    currencyName = info.name + " Orb of Epiphany";
                }

                return ExileCurrency.Builder.of(id, currencyName, ItemReqs.INSTANCE.IS_GEAR)
                        .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
                        .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
                        .addRequirement(ItemReqs.INSTANCE.IS_RARITY.get(new RarityKeyInfo(IRarity.COMMON_ID)))
                        .rarity(IRarity.EPIC_ID)
                        .addAlwaysUseModification(ItemMods.INSTANCE.COMMON_TO_EPIC_WITH_TAG.get(info))
                        .potentialCost(15)
                        .weight(CodeCurrency.Weights.RARE)
                        .buildCurrency(get());
            });

    public ExileKeyMap<ExileCurrency, NamedKey> PERFECTED_ORB_WITH_TAG = new ExileKeyMap<ExileCurrency, NamedKey>(get(), "perfected_orb_with_tag")
            .ofList(Arrays.asList(
                    new NamedKey(SlotTags.intelligence.GUID(), "Intelligence"),
                    new NamedKey(SlotTags.strength.GUID(), "Strength"),
                    new NamedKey(SlotTags.dexterity.GUID(), "Dexterity")
            ))
            .build((id, info) -> {

                obeliskOnlyDrop(id);

                String currencyName;
                if (info.GUID().equals(SlotTags.intelligence.GUID())) {
                    currencyName = "Perfected Orb of the Aegis";
                } else if (info.GUID().equals(SlotTags.strength.GUID())) {
                    currencyName = "Perfected Orb of the Bulwark";
                } else if (info.GUID().equals(SlotTags.dexterity.GUID())) {
                    currencyName = "Perfected Orb of the Phantom";
                } else {
                    currencyName = "Perfected " + info.name + " Orb";
                }

                return ExileCurrency.Builder.of(id, currencyName, ItemReqs.INSTANCE.IS_GEAR)
                        .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
                        .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
                        .addRequirement(ItemReqs.INSTANCE.IS_RARITY.get(new RarityKeyInfo(IRarity.COMMON_ID)))
                        .addRequirement(ItemReqs.INSTANCE.LEVEL_AT_LEAST_50)
                        .rarity(IRarity.MYTHIC_ID)
                        .addModification(ItemMods.INSTANCE.COMMON_TO_MYTHIC_WITH_TAG.get(info), 75)
                        .addModification(Modifications.INSTANCE.DESTROY_ITEM, 25)
                        .potentialCost(45)
                        .weight(0)
                        .buildCurrency(get());
            });

    @Override
    public void init() {
    }

}
