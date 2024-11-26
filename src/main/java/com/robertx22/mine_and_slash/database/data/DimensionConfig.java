package com.robertx22.mine_and_slash.database.data;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;

public class DimensionConfig implements JsonExileRegistry<DimensionConfig>, IAutoGson<DimensionConfig> {

    public static String DEFAULT_ID = "default";

    public DimensionConfig() {
    }

    public static DimensionConfig EMPTY = new DimensionConfig();

    public DimensionConfig(int startlvl, String dimension_id) {
        this.min_lvl = startlvl;
        this.dimension_id = dimension_id;
    }

    public DimensionConfig(String dimension_id, int min_lvl, int max_lvl) {
        this.dimension_id = dimension_id;
        this.min_lvl = min_lvl;
        this.max_lvl = max_lvl;
    }

    public static DimensionConfig Overworld() {
        DimensionConfig c = new DimensionConfig(1, "minecraft:overworld");
        c.min_lvl = 1;
        c.max_lvl = 50;
        return c;
    }

    public static DimensionConfig Nether() {
        DimensionConfig d = new DimensionConfig(10, "minecraft:the_nether").setMobTier(2);
        d.min_lvl = 25;
        d.max_lvl = 100;
        return d;
    }

    public static DimensionConfig End() {
        DimensionConfig d = new DimensionConfig(10, "minecraft:the_end").setMobTier(3);
        d.min_lvl = 95;
        d.max_lvl = 100;
        return d;
    }

    public DimensionConfig setDistPerLevel(int dist) {
        this.mob_lvl_per_distance = dist;
        return this;
    }

    public static DimensionConfig DefaultExtra() {
        DimensionConfig config = new DimensionConfig();
        config.min_lvl = 1;
        config.max_lvl = 100;
        config.scale_to_nearest_player = true;
        return config;
    }

    public DimensionConfig setMobTier(int t) {
        this.mob_tier = t;
        return this;
    }

    public String dimension_id = "default";

    public int mob_tier = 0; // todo unused?

    public float all_drop_multi = 1F;
    public float exp_multi = 1F;

    //public float unique_gear_drop_multi = 1F;

    public float mob_strength_multi = 1F;

    public int min_lvl = 1;
    public int max_lvl = Integer.MAX_VALUE;
    public int mob_lvl_per_distance = 100;
    public int min_lvl_area = 100;
    public boolean scale_to_nearest_player = false;

    public EntityConfig.SpecialMobStats stats = new EntityConfig.SpecialMobStats();

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.DIMENSION_CONFIGS;
    }

    @Override
    public String GUID() {
        return dimension_id;
    }

    @Override
    public int Weight() {
        return 1;
    }


    @Override
    public Class<DimensionConfig> getClassForSerialization() {
        return DimensionConfig.class;
    }
}
