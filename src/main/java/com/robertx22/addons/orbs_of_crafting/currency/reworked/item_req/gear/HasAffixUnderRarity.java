package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.GearRequirement;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class HasAffixUnderRarity extends GearRequirement {


    public Data data;

    public static record Data(String rar) {
        GearRarity getRarity() {
            return ExileDB.GearRarities().get(rar);
        }
    }

    public HasAffixUnderRarity(String id, Data data) {
        super("has_rar_affix_or_lower", id);
        this.data = data;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return HasAffixUnderRarity.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.getRarity().coloredName());
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Must have %1$s Affix or Lower")
                );
    }

    @Override
    public boolean isGearValid(ItemStack stack) {
        ExileStack ex = ExileStack.of(stack);
        var gear = ex.get(StackKeys.GEAR).get();
        return gear.affixes.getPrefixesAndSuffixes().stream().anyMatch(x -> data.getRarity().item_tier >= x.getRarity().item_tier);
    }
}
