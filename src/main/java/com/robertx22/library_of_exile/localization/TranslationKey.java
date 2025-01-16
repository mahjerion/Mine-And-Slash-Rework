package com.robertx22.library_of_exile.localization;

public class TranslationKey {

    public static String of(String modid, String path, String id) {
        return modid + "." + path + "." + id;
    }

    public static String ofDesc(String modid, String path, String id) {
        return modid + "." + path + ".desc." + id;
    }
}
