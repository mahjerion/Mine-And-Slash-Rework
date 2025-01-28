package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.GearRequirement;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
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

public class IsRarityReq extends GearRequirement {


    public Data data;

    public static record Data(String rarity) {
    }

    public IsRarityReq(String id, Data data) {
        super(ItemReqSers.IS_RARITY, id);
        this.data = data;
    }

    public GearRarity getRarity() {
        return ExileDB.GearRarities().get(data.rarity);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsRarityReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(getRarity().locName().withStyle(getRarity().textFormatting()));
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Must be %1$s Rarity")
                );
    }


    @Override
    public boolean isGearValid(ItemStack stack) {
        ExileStack ex = ExileStack.of(stack);

        return ex.get(StackKeys.GEAR).hasAndTrue(x -> x.rar.equals(data.rarity));
    }
}
