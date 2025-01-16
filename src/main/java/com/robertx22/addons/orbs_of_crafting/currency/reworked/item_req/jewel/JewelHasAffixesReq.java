package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.jewel;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.JewelRequirement;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;

public class JewelHasAffixesReq extends JewelRequirement {
    public JewelHasAffixesReq(String id) {
        super(ItemReqSers.JEWEL_HAS_AFFIXES, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return JewelHasAffixesReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public boolean isJewelValid(ExileStack data) {
        return !data.get(StackKeys.JEWEL).get().affixes.isEmpty();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, "Jewel Has Affixes")
                );
    }

}
