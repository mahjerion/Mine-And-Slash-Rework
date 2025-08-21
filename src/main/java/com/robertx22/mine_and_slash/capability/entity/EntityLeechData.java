package com.robertx22.mine_and_slash.capability.entity;

import com.robertx22.mine_and_slash.aoe_data.database.stats.ResourceStats;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.uncommon.MathHelper;

import java.util.HashMap;
import java.util.Map;

public class EntityLeechData {


    private HashMap<ResourceType, Float> map = new HashMap<>();

    public void addLeech(ResourceType type, float num) {
        if (!map.containsKey(type)) {
            map.put(type, 0f);
        }
        float fi = num + map.get(type);

        map.put(type, fi);
    }

    // todo implement expiration after 5s
    public void onSecondUseLeeches(EntityData data) {

        // 1) Clamp stored leech per resource to 5s of cap
        for (Map.Entry<ResourceType, Float> en : map.entrySet()) {
            float capPctPerSec = 5F * data.getUnit()
                .getCalculatedStat(ResourceStats.LEECH_CAP.get(en.getKey()))
                .getValue() / 100F;

            float maxPerSecond = data.getResources().getMax(data.entity, en.getKey()) * capPctPerSec;
            float clamped = MathHelper.clamp(en.getValue(), 0, maxPerSecond);
            map.put(en.getKey(), clamped);
        }

        // 2) Apply per-resource leech once (and debug)
        boolean anyLeechThisSecond = false;

        for (Map.Entry<ResourceType, Float> entry : map.entrySet()) {
            ResourceType rtype = entry.getKey();

            float capPctPerSec = data.getUnit()
                .getCalculatedStat(ResourceStats.LEECH_CAP.get(rtype))
                .getValue() / 100F;

            float reservoir = entry.getValue(); // stored leech for this resource
            if (reservoir > 1f) { // tiny cutoff stays
                float maxRes = data.getResources().getMax(data.entity, rtype);
                float perSecondCap = capPctPerSec * maxRes;

                // Intended drain this second (bounded by per-second cap and reservoir)
                float take = Math.min(reservoir, perSecondCap);

                // --- talent/ascendancy override (wire this when ready) ---
                // boolean allowFullLeech = data.getUnit()
                //     .getCalculatedStat(Stats.LEECH_AT_FULL_HEALTH).getValue() > 0;
                // If you don't have the stat yet, keep false for now:
                 boolean allowFullLeech = false;

                // Apply and get what actually landed
                float applied = data.getResources().restoreAndReturnApplied(
                    data.entity,
                    rtype,
                    take,
                    com.robertx22.mine_and_slash.uncommon.effectdatas.rework.RestoreType.leech
                );

                // If HEALTH is full and nothing applied, kill health leech unless allowed
                if (rtype == ResourceType.health && applied <= 0f && !allowFullLeech) {
                    map.put(rtype, 0f);
                    continue;
                }

                // Some leech occurred on this resource this second
                anyLeechThisSecond = true;

                // Debug only when something actually healed (keeps chat clean)
                if (applied > 0f && data.entity instanceof net.minecraft.server.level.ServerPlayer sp) {
                    com.robertx22.mine_and_slash.event_hooks.my_events.LeechDebug.tick(sp, rtype, applied);
                }

                // **Critical**: drain by 'take' (not by 'applied') to preserve ≤5s duration
                addLeech(rtype, -take);
            }
        }

        // STOP: no leech at all this second → remove the test buff and end lifecycle debug
        if (!anyLeechThisSecond && data.entity instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.robertx22.mine_and_slash.event_hooks.my_events.LeechDebug.maybeStop(sp);
        }
    }
}
