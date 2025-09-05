package com.robertx22.mine_and_slash.capability.entity;

import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;

/*
 * Accumulates resource LOSS per type (spend, drains, damage, etc).
 * Call addLoss(...) whenever a resource actually decreases.
 * Use consumeThresholds(...) / addAndConsumeForKey(...) to fire effects and keep remainder.
 */
public class ResourceTracker {
    private static final float EPS = 1e-4f;
    private static final float DEFAULT_KEY_PROGRESS = 0f;

    private final java.util.EnumMap<ResourceType, Float> lost = new java.util.EnumMap<>(ResourceType.class);

    public void addLoss(ResourceType rt, float amount) {
        if (amount <= 0f) return;
        lost.merge(rt, amount, Float::sum);
    }

    public float getLoss(ResourceType rt) {
        return lost.getOrDefault(rt, 0f);
    }

    public int consumeThresholds(ResourceType type, float threshold) {
        if (threshold <= 0f) return 0;
        float have = lost.getOrDefault(type, 0f);
        if (have + EPS < threshold) return 0;

        int procs = (int) Math.floor((have + EPS) / threshold);
        float remainder = have - procs * threshold;

        if (remainder <= EPS) lost.remove(type);
        else lost.put(type, remainder);
        return procs;
    }

    public int consumeThresholdsAcross(java.util.Set<ResourceType> types, float threshold) {
        if (threshold <= 0f || types == null || types.isEmpty()) return 0;

        int procs = 0;

        while (total(types) + EPS >= threshold) {
            float need = threshold;

            for (ResourceType rt : types) {
                float have = lost.getOrDefault(rt, 0f);
                if (have <= 0f) continue;

                float take = Math.min(have, need);
                if (take > 0f) {
                    float remaining = have - take;
                    if (remaining <= EPS) lost.remove(rt);
                    else lost.put(rt, remaining);
                    need -= take;
                }
                if (need <= EPS) break;
            }
            procs++;
        }

        for (ResourceType rt : types) {
            if (lost.getOrDefault(rt, 0f) <= EPS) lost.remove(rt);
        }
        return procs;
    }

    private float total(java.util.Set<ResourceType> types) {
        float sum = 0f;
        for (ResourceType rt : types) sum += lost.getOrDefault(rt, 0f);
        return sum;
    }

    private final java.util.EnumMap<ResourceType, java.util.Map<String, Float>> keyProgress =
        new java.util.EnumMap<>(ResourceType.class);

    private java.util.Map<String, Float> getKeyProgressOrCreate(ResourceType rt) {
        return keyProgress.computeIfAbsent(rt, __ -> new java.util.HashMap<>());
    }
  
    public void clearKey(ResourceType rt, String key) {
            if (key == null || key.isEmpty()) return;
            var byKey = keyProgress.get(rt);
            if (byKey == null) return;
            byKey.remove(key);
            if (byKey.isEmpty()) {
                keyProgress.remove(rt);
            }
        }

    public int addAndConsumeForKey(String key, ResourceType rt, float add, float threshold) {
        if (key == null || key.isEmpty() || add <= 0f || threshold <= 0f) return 0;

        var byKey = getKeyProgressOrCreate(rt);
        float cur = byKey.getOrDefault(key, DEFAULT_KEY_PROGRESS) + add;

        int procs = 0;
        while (cur + EPS >= threshold) {
            cur -= threshold;
            procs++;
        }
        if (cur <= EPS) byKey.remove(key);
        else byKey.put(key, cur);

        return procs;
    }

    public float getKeyProgress(String key, ResourceType rt) {
        var byKey = keyProgress.get(rt);
        return byKey == null ? DEFAULT_KEY_PROGRESS : byKey.getOrDefault(key, DEFAULT_KEY_PROGRESS);
    }


    public void setKeyProgress(String key, ResourceType rt, float value) {
        if (key == null || key.isEmpty()) return;
        var byKey = keyProgress.computeIfAbsent(rt, __ -> new java.util.HashMap<>());
        float val = Math.max(0f, value);
        if (val <= EPS) byKey.remove(key); else byKey.put(key, val);
        if (byKey.isEmpty()) keyProgress.remove(rt);
    }

    public float decayKeyProgress(String key, ResourceType rt, float amount) {
        if (key == null || key.isEmpty() || amount <= 0f) return getKeyProgress(key, rt);
        var byKey = keyProgress.get(rt);
        if (byKey == null) return 0f;
        float cur = byKey.getOrDefault(key, 0f);
        float next = Math.max(0f, cur - amount);
        if (next <= EPS) byKey.remove(key); else byKey.put(key, next);
        if (byKey.isEmpty()) keyProgress.remove(rt);
        return next;
    }

    public void clear(ResourceType rt) {
        lost.remove(rt);
    }

    public void clearAll() {
        lost.clear();
    }
}
