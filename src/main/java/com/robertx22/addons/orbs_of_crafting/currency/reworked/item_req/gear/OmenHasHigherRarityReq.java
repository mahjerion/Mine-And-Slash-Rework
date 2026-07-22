package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.OmenRequirement;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.omen.OmenData;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;

public class OmenHasHigherRarityReq extends OmenRequirement {

    public OmenHasHigherRarityReq(String id) {
        super(ItemReqSers.OMEN_HAS_HIGHER_RAR, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return OmenHasHigherRarityReq.class;
    }

    @Override
    public boolean isOmenValid(OmenData omen) {
        return omen.getRarity().hasHigherRarity();
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Must be below maximum Rarity"));
    }

}
