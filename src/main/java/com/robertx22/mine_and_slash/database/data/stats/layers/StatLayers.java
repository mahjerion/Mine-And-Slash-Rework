package com.robertx22.mine_and_slash.database.data.stats.layers;

import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.uncommon.STATICS;

public class StatLayers {

    public static class Offensive {


        public static StatLayer FLAT_DAMAGE = new StatLayer("flat_damage", "Flat Damage", StatLayer.LayerAction.ADD, 0, -STATICS.MAX_FLOAT, STATICS.MAX_FLOAT);
        public static StatLayer DAMAGE_CONVERSION = new StatLayer("damage_conversion", "%1$s to %2$s Conversion", StatLayer.LayerAction.CONVERT_PERCENT, 1, 0, 100);
        public static StatLayer ELEMENT_AS_EXTRA_OTHER_FLAT_DAMAGE = new StatLayer("ele_as_extra_flat", "Plus %1$s as Extra %2$s", StatLayer.LayerAction.X_AS_BONUS_Y_ELEMENT_DAMAGE, 2, 0, 100);

        public static StatLayer ADDITIVE_DMG = new StatLayer("additive_damage", "Additive Damage", StatLayer.LayerAction.MULTIPLY, 3, -1, STATICS.MAX_FLOAT);
        public static StatLayer DOT_DMG_MULTI = new StatLayer("dot_dmg_multi", "DOT Damage Multi", StatLayer.LayerAction.MULTIPLY, 5, -1, STATICS.MAX_FLOAT);
        public static StatLayer CRIT_DAMAGE = new StatLayer("crit_damage", "Crit Damage", StatLayer.LayerAction.MULTIPLY, 7, -1, STATICS.MAX_FLOAT);
        public static StatLayer DOUBLE_DAMAGE = new StatLayer("double_damage", "Double Damage", StatLayer.LayerAction.MULTIPLY, 9, 2, 2);

        public static void init() {


        }
    }

    public static class Defensive {

        // todo does this work its still confusing and probably buggy!!!

        public static StatLayer PHYS_MITIGATION = new StatLayer("physical_mitigation", "Physical Mitigation", StatLayer.LayerAction.MULTIPLY, 100, 0.1F, STATICS.MAX_FLOAT);
        public static StatLayer ELEMENTAL_MITIGATION = new StatLayer("elemental_mitigation", "Elemental Mitigation", StatLayer.LayerAction.MULTIPLY, 101, 0.1F, STATICS.MAX_FLOAT);
        public static StatLayer DAMAGE_REDUCTION = new StatLayer("damage_reduction", "Damage Reduction", StatLayer.LayerAction.MULTIPLY, 102, 0.5F, STATICS.MAX_FLOAT);
        public static StatLayer DAMAGE_SUPPRESSION = new StatLayer("damage_suppression", "Damage Suppression", StatLayer.LayerAction.MULTIPLY, 103, 0.5F, 1);

        public static StatLayer FLAT_DAMAGE_REDUCTION = new StatLayer("flat_damage_reduction", "Flat Damage Reduction", StatLayer.LayerAction.ADD, 200, -1000, STATICS.MAX_FLOAT);

        public static void init() {


        }

    }


    public static class Misc {

        public static void init() {


        }
    }

    public static void init() {

        Defensive.init();
        Offensive.init();
        Misc.init();

    }

    public static void register() {

        for (StatLayer a : StatLayer.ALL) {
            a.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        }

    }
}
