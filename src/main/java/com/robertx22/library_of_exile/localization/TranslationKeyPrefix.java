package com.robertx22.library_of_exile.localization;


import com.robertx22.library_of_exile.registry.IGUID;

public class TranslationKeyPrefix implements IGUID {

    String id;

    public TranslationKeyPrefix(String id) {
        this.id = id;
    }

    @Override
    public String GUID() {
        return id;
    }
}
