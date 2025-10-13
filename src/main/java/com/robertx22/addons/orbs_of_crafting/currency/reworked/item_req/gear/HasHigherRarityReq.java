package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.GearRequirement;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.MapRequirement;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class HasHigherRarityReq extends GearRequirement {

    public HasHigherRarityReq(String id) {
        super(ItemReqSers.HAS_HIGHER_RAR, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return HasHigherRarityReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public boolean isGearValid(ItemStack stack) {
        var data = ExileStack.of(stack).get(StackKeys.GEAR).get();
        return data.getRarity().hasHigherRarity() && data.lvl >= data.getRarity().getHigherRarity().min_lvl;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Must be below maximum Rarity")
                );
    }

}
