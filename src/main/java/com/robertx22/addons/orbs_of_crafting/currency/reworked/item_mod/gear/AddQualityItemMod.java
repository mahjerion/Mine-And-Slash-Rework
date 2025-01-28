package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.GearModification;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.MinMax;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class AddQualityItemMod extends GearModification {

    public Data data;

    public static record Data(MinMax add_quality) {
    }

    public AddQualityItemMod(String id, Data data) {
        super(ItemModificationSers.ADD_QUALITY, id);
        this.data = data;
    }

    @Override
    public void modifyGear(ExileStack stack, ItemModificationResult r) {
        stack.get(StackKeys.CUSTOM).edit(gear -> {
            gear.data.set(CustomItemData.KEYS.QUALITY, gear.data.get(CustomItemData.KEYS.QUALITY) + data.add_quality.random());
        });
    }


    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public MutableComponent getDescWithParams() {
        String text = data.add_quality.min + "";
        if (data.add_quality.hasRange()) {
            text = data.add_quality.min + " - " + data.add_quality.max;
        }
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(text);
    }


    @Override
    public Class<?> getClassForSerialization() {
        return AddQualityItemMod.class;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Add %1$s Quality")
                );
    }


}
