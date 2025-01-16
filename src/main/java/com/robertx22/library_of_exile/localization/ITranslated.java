package com.robertx22.library_of_exile.localization;

public interface ITranslated {

    public TranslationBuilder createTranslationBuilder();

    public default ExileTranslation getTranslation(TranslationType type) {
        return createTranslationBuilder().all.get(type);
    }
}
