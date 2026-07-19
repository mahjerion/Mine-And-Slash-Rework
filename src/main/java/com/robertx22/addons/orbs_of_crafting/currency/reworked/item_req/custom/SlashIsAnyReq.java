package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.custom;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.register.reqs.IsAnyReq;

public class SlashIsAnyReq extends IsAnyReq {

    private final String desc;

    public SlashIsAnyReq(String id, Data data, String desc) {
        super(id, data, desc);
        this.desc = desc;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return SlashIsAnyReq.class;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, desc));
    }
}