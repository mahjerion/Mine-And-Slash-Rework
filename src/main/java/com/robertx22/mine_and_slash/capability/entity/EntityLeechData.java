package com.robertx22.mine_and_slash.capability.entity;

import com.robertx22.mine_and_slash.aoe_data.database.stats.ResourceStats;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.uncommon.MathHelper;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds pending leech “reservoirs” per resource and applies them once per second.
 *
 * Design notes:
 * - Clamp each reservoir to “≤ 5 seconds worth of per-second cap”.
 * - Drain by the intended ‘take’ (min(reservoir, perSecondCap)), not by what was actually applied,
 *   so duration semantics remain consistent even if the target is capped/full.
 * - Prune tiny leftovers to keep the map small.
 */
public class EntityLeechData {

    private static final float EPS = 0.1f; // tiny cutoff to treat as zero
    private final EnumMap<ResourceType, Float> store = new EnumMap<>(ResourceType.class);

    /** Adds (or subtracts) pending leech for a resource. */
    public void addLeech(ResourceType type, float amount) {
        store.merge(type, amount, Float::sum);
        // prune tiny / negative leftovers
        if (store.getOrDefault(type, 0f) <= EPS) {
            store.remove(type);
        }
    }

    /**
     * Called once per second. Applies up to the per-second cap for each resource,
     * then drains the reservoir by the amount we *intended* to take.
     */
    public void onSecondUseLeeches(EntityData data) {

        // 1) Clamp stored leech per resource to ≤ 5s of cap (prevents unbounded queues)
        for (Map.Entry<ResourceType, Float> en : store.entrySet()) {
            ResourceType rt = en.getKey();
            float capPctPerSec = data.getUnit()
                    .getCalculatedStat(ResourceStats.LEECH_CAP.get(rt))
                    .getValue() / 100F;

            float maxRes   = data.getResources().getMax(data.entity, rt);
            float fiveSecs = 5F * capPctPerSec * maxRes;   // “5 seconds worth” reservoir cap
            float clamped  = MathHelper.clamp(en.getValue(), 0, fiveSecs);
            en.setValue(clamped);
        }

        // 2) Apply per-resource leech once
        for (Map.Entry<ResourceType, Float> entry : store.entrySet()) {
            ResourceType rt   = entry.getKey();
            float reservoir   = entry.getValue();
            if (reservoir <= EPS) continue;

            float capPctPerSec = data.getUnit()
                    .getCalculatedStat(ResourceStats.LEECH_CAP.get(rt))
                    .getValue() / 100F;

            float maxRes       = data.getResources().getMax(data.entity, rt);
            float perSecondCap = capPctPerSec * maxRes;

            // Intended drain this second (bounded by per-second cap and reservoir)
            float take = Math.min(reservoir, perSecondCap);
            if (take <= EPS) continue;

            // Hook: a future stat could allow full-health leeching
            final boolean allowFullLeech = data.getUnit()
                .getCalculatedStat(ResourceStats.LEECH_AT_FULL_HEALTH.get()).getValue() > 0;

            // Apply and get what actually landed
            float applied = data.getResources().restoreAndReturnApplied(
                    data.entity, rt, take,
                    com.robertx22.mine_and_slash.uncommon.effectdatas.rework.RestoreType.leech
            );

            // Full-resource policy:
            // - Non-health: never persist at full → discard.
            // - Health: persist only if 'leech_at_full_health' is enabled.
            // If nothing landed (resource is full), enforce full-resource policy.
            if (applied <= EPS) { // use EPS to avoid float noise
                boolean keepReservoir =
                    (rt == ResourceType.health) && allowFullLeech; // only health with talent

                if (!keepReservoir) {
                    entry.setValue(0f); // discard reservoir
                }
                continue; // skip draining by 'take'
            }


            // Normal path: drain by intended 'take' to preserve ≤5s duration
            entry.setValue(reservoir - take);
        }

        // 3) Prune empty entries to keep the map small
        store.entrySet().removeIf(e -> e.getValue() <= EPS);
    }
}

