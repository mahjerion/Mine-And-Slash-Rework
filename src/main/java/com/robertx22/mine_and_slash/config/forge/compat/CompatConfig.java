package com.robertx22.mine_and_slash.config.forge.compat;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;

// todo write wiki docs for the new preset
public class CompatConfig {
    public static final ForgeConfigSpec spec;
    public static final CompatConfig CONTAINER;

    public static CompatDummy get() {
        if (!IRestrictedConfig.compatModeIsInstalled()) {
            return CompatConfigPreset.ORIGINAL_MODE.defaults;
        }
        var c = CONTAINER.COMPATIBILITY_PRESETS.get().getReal();
        return CONTAINER.map.get(c);
    }

    static {
        final Pair<CompatConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(b -> new CompatConfig(b));
        spec = specPair.getRight();
        CONTAINER = specPair.getLeft();
    }


    private HashMap<CompatConfigPreset, CompatData> map = new HashMap<>();

    private ForgeConfigSpec.EnumValue<CompatConfigPreset> COMPATIBILITY_PRESETS;


    CompatConfig(ForgeConfigSpec.Builder b) {


        b.comment("Compatibility Presets. Unlocked by installing the Compatibility addon");


        if (IRestrictedConfig.compatModeIsInstalled()) {

            COMPATIBILITY_PRESETS = b.defineEnum("COMPATIBILITY_PRESETS", CompatConfigPreset.ORIGINAL_MODE);

            b.comment("It's advised to ONLY pick a COMPATIBILITY_PRESETS and not mess further with the configs.")
                    .push("compatibility_configs");

            for (CompatConfigPreset preset : CompatConfigPreset.values()) {
                b.comment(preset.comment);
                b.push(preset.name());

                var data = new CompatData();
                data.build(b, preset.defaults);
                map.put(preset, data);

                b.pop();
            }
        } else {
            b.define("addon_link", "https://www.curseforge.com/minecraft/mc-mods/mine-and-slash-compatibility");
        }

    }


}
