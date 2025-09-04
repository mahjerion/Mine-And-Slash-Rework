package com.robertx22.mine_and_slash.capability.entity;

import com.robertx22.mine_and_slash.aoe_data.database.stats.ResourceStats;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.uncommon.MathHelper;

import java.util.EnumMap;
import java.util.Map;

public class EntityLeechData {

    private static final float EPS = 0.1f;
    private final EnumMap<ResourceType, Float> store = new EnumMap<>(ResourceType.class);

    public void addLeech(ResourceType type, float amount) {
        store.merge(type, amount, Float::sum);
        if (store.getOrDefault(type, 0f) <= EPS) {
            store.remove(type);
        }
    }

    public void onSecondUseLeeches(EntityData data) {

        for (Map.Entry<ResourceType, Float> en : store.entrySet()) {
            ResourceType rt = en.getKey();
            float capPercentPerSec = data.getUnit()
                    .getCalculatedStat(ResourceStats.LEECH_CAP.get(rt))
                    .getValue() / 100F;

            float maxRes   = data.getResources().getMax(data.entity, rt);
            float fiveSecs = 5F * capPercentPerSec * maxRes;   // “5 seconds worth” reservoir cap
            float clamped  = MathHelper.clamp(en.getValue(), 0, fiveSecs);
            en.setValue(clamped);
        }

        for (Map.Entry<ResourceType, Float> entry : store.entrySet()) {
            ResourceType rt   = entry.getKey();
            float reservoir   = entry.getValue();
            if (reservoir <= EPS) continue;

            float capPercentPerSec = data.getUnit()
                    .getCalculatedStat(ResourceStats.LEECH_CAP.get(rt))
                    .getValue() / 100F;

            float maxRes       = data.getResources().getMax(data.entity, rt);
            float perSecondCap = capPercentPerSec * maxRes;

            float take = Math.min(reservoir, perSecondCap);
            if (take <= EPS) continue;

            // Hook: a future stat could allow full-health leeching
            final boolean allowFullLeech = data.getUnit()
                .getCalculatedStat(ResourceStats.LEECH_AT_FULL_HEALTH.get()).getValue() > 0;

            float applied = data.getResources().restoreAndReturnApplied(
                    data.entity, rt, take,
                    com.robertx22.mine_and_slash.uncommon.effectdatas.rework.RestoreType.leech
            );

            if (applied <= EPS) {
                boolean keepReservoir =
                    (rt == ResourceType.health) && allowFullLeech; // only health with talent

                if (!keepReservoir) {
                    entry.setValue(0f); // discard reservoir
                }
                continue;
            }


            entry.setValue(reservoir - take);
        }

        store.entrySet().removeIf(e -> e.getValue() <= EPS);
    }
}

