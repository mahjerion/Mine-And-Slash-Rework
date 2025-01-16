package com.robertx22.orbs_of_crafting.register.mods;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.main.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import com.robertx22.orbs_of_crafting.register.mods.base.VanillaItemModSers;
import net.minecraft.network.chat.MutableComponent;

public class DoNothingItemMod extends ItemModification {

    public DoNothingItemMod(String id) {
        super(VanillaItemModSers.DO_NOTHING, id);
    }


    @Override
    public Class<?> getClassForSerialization() {
        return DoNothingItemMod.class;
    }


    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {

    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.NEUTRAL;
    }


    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, "Does Nothing"));
    }

}
