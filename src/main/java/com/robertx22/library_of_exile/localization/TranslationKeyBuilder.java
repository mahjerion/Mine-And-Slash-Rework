package com.robertx22.library_of_exile.localization;

public class TranslationKeyBuilder {

    public String modid;

    public TranslationKeyBuilder(String modid) {
        this.modid = modid;
    }

    public String of(TranslationKeyPrefix prefix, String guid) {
        return modid + "." + prefix.GUID() + "." + guid;
    }
}
