package com.robertx22.library_of_exile.localization;

import com.robertx22.library_of_exile.registry.ExileRegistry;
import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

public class ExileTranslation {

    public String key;
    public String locname;

    public ExileTranslation(String key, String locname) {
        this.key = key;
        this.locname = locname;
    }

    public static ExileTranslation registry(String modid, ExileRegistry<?> reg, String locname) {
        return new ExileTranslation(modid + "." + reg.getExileRegistryType().idWithoutModid + "." + reg.GUID(), locname);
    }


    public static ExileTranslation of(String id, String locname) {
        return new ExileTranslation(id, locname);
    }


    public static ExileTranslation item(Item item, String locname) {
        String id = VanillaUTIL.REGISTRY.items().getKey(item).toString();
        return new ExileTranslation("item." + id, locname);
    }

    public static ExileTranslation block(Block item, String locname) {
        String id = VanillaUTIL.REGISTRY.blocks().getKey(item).toString();
        return new ExileTranslation("block." + id, locname);
    }

    public static ExileTranslation mobEffect(MobEffect item, String locname) {
        String id = BuiltInRegistries.MOB_EFFECT.getKey(item).toString();
        return new ExileTranslation("block." + id, locname);
    }

    public String getFormatedForLangFile() {
        return key.replaceAll(" ", "_").toLowerCase(Locale.ROOT).replaceAll("/", ".").replaceAll(":", ".");
    }

    public MutableComponent getTranslatedName(Object... objects) {
        return Component.translatable(key, objects);
    }
}

