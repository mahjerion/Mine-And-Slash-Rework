package com.robertx22.orbs_of_crafting.lang;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.orbs_of_crafting.main.OrbsRef;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

public enum OrbWords implements ITranslated {
    Currency("Currency"),
    THIS_IS_NOT_A("This is not a %1$s"),
    POTENTIAL_COST("Potential Cost: %1$s"),
    NOT_A_POTENTIAL_CONSUMER("Does not consume potential when used"),
    Requirements("Requirements: "),
    OUTCOME_TIP("%1$s - %2$s"),
    ALWAYS_DOES("Always does:"),
    RANDOM_OUTCOME("Random Outcome:"),
    ;
    public String name;

    OrbWords(String name) {
        this.name = name;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return new TranslationBuilder(OrbsRef.MODID).name(ExileTranslation.of(OrbsRef.MODID + ".words." + this.name().toLowerCase(Locale.ROOT), name));
    }

    public MutableComponent get(Object... obj) {
        return getTranslation(TranslationType.NAME).getTranslatedName(obj);
    }
}
