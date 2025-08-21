package com.robertx22.mine_and_slash.aoe_data.database.stats.custom;

import com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition.HasExileEffectCondition;

public final class ConditionalStats {
    private ConditionalStats() {}

    // Any-resource leeching
    public static Stat whileLeeching(String id, String name, Stat base) {
        return new ConditionalStat(id, name, base,
            new HasExileEffectCondition(ModEffects.LEECHING_STATE));
    }

    // Per-resource leeching
    public static Stat whileLeeching(String id, String name, Stat base, ResourceType rt) {
        return new ConditionalStat(id, name, base,
            new HasExileEffectCondition(ModEffects.LEECHING_STATE_BY_RES.get(rt)));
    }

    // Any-resource regen
    public static Stat whileRegen(String id, String name, Stat base) {
        return new ConditionalStat(id, name, base,
            new HasExileEffectCondition(ModEffects.REGEN_STATE));
    }

    // Per-resource regen
    public static Stat whileRegen(String id, String name, Stat base, ResourceType rt) {
        return new ConditionalStat(id, name, base,
            new HasExileEffectCondition(ModEffects.REGEN_STATE_BY_RES.get(rt)));
    }
}
