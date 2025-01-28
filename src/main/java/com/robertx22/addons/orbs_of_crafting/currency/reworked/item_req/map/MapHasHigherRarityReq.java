package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.map;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.MapRequirement;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;

public class MapHasHigherRarityReq extends MapRequirement {

    public MapHasHigherRarityReq(String id) {
        super(ItemReqSers.MAP_HAS_HIGHER_RAR, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return MapHasHigherRarityReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public boolean isMapValid(ExileStack stack) {
        var data = stack.get(StackKeys.MAP).get();
        if (!data.getRarity().hasHigherRarity()) {
            return false;
        }
        if (data.lvl >= data.getRarity().getHigherRarity().min_lvl == false) {
            return false;
        }
        return true;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Map Rarity Must be lower than Mythic and Map Level high enough for the Rarity upgrade")
                );
    }

}
