package com.robertx22.library_of_exile.localization;

import java.util.HashMap;
import java.util.Map;

public class TranslationBuilder {

    public String modid;
    public HashMap<TranslationType, ExileTranslation> all = new HashMap<>();

    public TranslationBuilder(String modid) {
        this.modid = modid;
    }

    public static TranslationBuilder of(String modid) {
        return new TranslationBuilder(modid);
    }

    public TranslationBuilder name(ExileTranslation tra) {
        all.put(TranslationType.NAME, tra);
        return this;
    }

    public TranslationBuilder desc(ExileTranslation tra) {
        all.put(TranslationType.DESCRIPTION, tra);
        return this;
    }

    public void build() {

        if (!ExileLangFile.all.containsKey(modid)) {
            ExileLangFile.all.put(modid, new HashMap<>());
        }
        for (Map.Entry<TranslationType, ExileTranslation> en : all.entrySet()) {
            var tra = en.getValue();
            ExileLangFile.all.get(modid).put(tra.key, tra);
        }
    }
}
