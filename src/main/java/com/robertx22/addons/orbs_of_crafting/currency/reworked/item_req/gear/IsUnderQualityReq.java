package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.GearRequirement;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class IsUnderQualityReq extends GearRequirement {

    public static Data UNDER_20 = new Data(20);
    public static Data UNDER_21 = new Data(21);

    public Data data;

    public static record Data(int max_quality) {
    }

    public IsUnderQualityReq(String id, Data data) {
        super(ItemReqSers.IS_UNDER_QUALITY, id);
        this.data = data;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsUnderQualityReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.max_quality);
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Must be under %1$s Quality")
                );
    }


    @Override
    public boolean isGearValid(ItemStack stack) {
        ExileStack ex = ExileStack.of(stack);

        return ex.get(StackKeys.CUSTOM).getOrCreate().data.get(CustomItemData.KEYS.QUALITY) < data.max_quality;
    }
}
