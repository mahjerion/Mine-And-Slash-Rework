package com.robertx22.mine_and_slash.config.forge.compat;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;

public class CompatConfig {
    public static final ForgeConfigSpec spec;
    public static final CompatConfig CONTAINER;

    public static CompatData get() {

        if (ModList.get().isLoaded("mns_compat")) {
            return CONTAINER.map.get(CompatConfigPreset.FULLY_COMPATIBLE);
        }

        return CONTAINER.map.get(CompatConfigPreset.ORIGINAL_OVERRIDE_MODE);

    }

    static {
        final Pair<CompatConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(b -> new CompatConfig(b));
        spec = specPair.getRight();
        CONTAINER = specPair.getLeft();
    }


    private HashMap<CompatConfigPreset, CompatData> map = new HashMap<>();

    CompatConfig(ForgeConfigSpec.Builder b) {

        b.comment("Compatibility presets: Fully compat mode means mine and slash will act as a nice mod to place in modpacks, it won't break anything and all other mods should work with it." +
                        "Original mode on the other hand makes mns override damage mechanics and other things, making it only really good for a modpack specifically created around mine and slash." +
                        "Installing the Mine and Slash Compatibility Addon mod enables the compatible mode preset")
                .push("compatibility_configs");


        for (CompatConfigPreset preset : CompatConfigPreset.values()) {

            b.comment(preset.comment);
            b.push(preset.name());

            var data = new CompatData();
            data.build(b, preset.defaults);
            map.put(preset, data);

            b.pop();
        }

    }


}
