package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.item_types;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.network.chat.MutableComponent;

public abstract class BeItemTypeRequirement extends ItemRequirement {

    public transient String locname;

    public BeItemTypeRequirement(String serializer, String id, String locname) {
        super(serializer, id);
        this.locname = locname;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return new TranslationBuilder(SlashRef.MODID).desc(ExileTranslation.registry(this, locname));
    }

}
