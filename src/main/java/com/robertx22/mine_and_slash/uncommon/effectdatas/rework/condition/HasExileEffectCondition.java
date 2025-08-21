package com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition;

import com.robertx22.mine_and_slash.aoe_data.database.stats.base.EffectCtx;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import net.minecraft.world.entity.LivingEntity;

/**
 * Gates a stat effect if the chosen side (source/target) currently has a given ExileEffect.
 * Works with our state flags like LEECHING_STATE / REGEN_STATE and their per-resource variants.
 */
public class HasExileEffectCondition extends StatCondition {

    // Serialized id of the ExileEffect to check (e.g., "leeching_state")
    public String effectId = "";

    // Required: serializer name/id
    private static final String SER_ID = "has_exile_effect";

    // Convenience ctor for code: pass an EffectCtx
    public HasExileEffectCondition(EffectCtx ctx) {
        super(SER_ID + "_" + ctx.resourcePath, SER_ID);
        this.effectId = ctx.resourcePath;
    }

    // Default no-arg ctor for (de)serialization
    public HasExileEffectCondition() {
        super("", SER_ID);
    }

    @Override
    public boolean can(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {
        if (effectId == null || effectId.isEmpty()) return false;

        ExileEffect fx = ExileDB.ExileEffects().get(effectId);
        if (fx == null) return false;

        // Pick which unit to test based on the side the stat is applied from
        LivingEntity unit = (statSource == EffectSides.Source) ? event.source : event.target;
        if (unit == null) return false;

        return Load.Unit(unit).getStatusEffectsData().has(fx);
    }

    @Override
    public Class<? extends StatCondition> getSerClass() {
        return HasExileEffectCondition.class;
    }
}

