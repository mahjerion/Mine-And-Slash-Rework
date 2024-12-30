package com.robertx22.mine_and_slash.config.forge.compat;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;

public class CompatConfig {
    public static final ForgeConfigSpec spec;
    public static final CompatConfig CONTAINER;

    public static CompatData get() {
        return CONTAINER.map.get(CONTAINER.COMPATIBILITY_PRESET.get());
    }

    static {
        final Pair<CompatConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(b -> new CompatConfig(b));
        spec = specPair.getRight();
        CONTAINER = specPair.getLeft();
    }


    public CompatConfigPreset getPreset() {
        return CONTAINER.COMPATIBILITY_PRESET.get();
    }

    public ForgeConfigSpec.EnumValue<CompatConfigPreset> COMPATIBILITY_PRESET;

    private HashMap<CompatConfigPreset, CompatData> map = new HashMap<>();

    CompatConfig(ForgeConfigSpec.Builder b) {

        b.comment("Compatibility Configs")
                .push("compatibility_configs");

        COMPATIBILITY_PRESET = b.comment("Pick a compatibility preset. Fully compat mode means mine and slash will act as a nice mod to place in modpacks, it won't break anything and all other mods should work with it." +
                        "Original mode on the other hand makes mns override damage mechanics and other things, making it only really good for a modpack specifically created around mine and slash." +
                        "Presets configs can be further tweaked.")
                .defineEnum("COMPATIBILITY_PRESET", CompatConfigPreset.FULLY_COMPATIBLE);


        for (CompatConfigPreset preset : CompatConfigPreset.values()) {

            b.push(preset.name());

            var data = new CompatData();
            data.build(b, preset.defaults);
            map.put(preset, data);

            b.pop();
        }

    }


}
