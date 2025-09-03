package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpendThresholdRuntime {
    // gameTime (ticks) when each key’s cooldown ends
    private final Map<String, Long> cooldownUntil = new HashMap<>();

    // last activity tick for each threshold key (progress added)
    private final Map<String, Long> lastActivityTick = new HashMap<>();

    // last decay tick applied for each key (so we decay at most once per second)
    private final Map<String, Long> lastDecayTick = new HashMap<>();

    // last integer progress sent to client (to throttle network updates)
    private final Map<String, Integer> lastProgressIntSent = new HashMap<>();

    // Index of active threshold keys by resource (only keys with progress > 0 or recently updated)
    private final java.util.EnumMap<ResourceType, Set<String>> activeByResource = new java.util.EnumMap<>(ResourceType.class);
    // Quick lookup of spec by key (used for decay threshold value)
    private final Map<String, SpendThresholdSpec> specByKey = new HashMap<>();

    public void startCooldown(String key, long now, int cooldownTicks) {
        if (cooldownTicks <= 0) return;
        cooldownUntil.put(key, now + cooldownTicks);
    }

    public boolean isCoolingDown(String key, long now) {
        Long until = cooldownUntil.get(key);
        return until != null && now < until;
    }

    /** Remaining ticks until ready (0 if no cooldown / already ready). */
    public int cooldownRemainingTicks(String key, long now) {
        Long until = cooldownUntil.get(key);
        if (until == null) return 0;
        long rem = until - now;
        return (int) Math.max(0, rem);
    }

    // === Activity/Decay tracking ===
    public void markActivity(String key, long now) {
        if (key == null || key.isEmpty()) return;
        lastActivityTick.put(key, now);
        lastDecayTick.remove(key);
    }

    public long getLastActivity(String key) {
        return lastActivityTick.getOrDefault(key, 0L);
    }

    public long getLastDecay(String key) {
        return lastDecayTick.getOrDefault(key, 0L);
    }

    public void markDecay(String key, long now) {
        if (key == null || key.isEmpty()) return;
        lastDecayTick.put(key, now);
    }

    public boolean progressIntChanged(String key, int intProgress) {
        Integer prev = lastProgressIntSent.get(key);
        if (prev == null || prev.intValue() != intProgress) {
            lastProgressIntSent.put(key, intProgress);
            return true;
        }
        return false;
    }

    // === Active key index ===
    public void markActive(ResourceType rt, String key, SpendThresholdSpec spec) {
        if (rt == null || key == null || key.isEmpty() || spec == null) return;
        activeByResource.computeIfAbsent(rt, __ -> new HashSet<>()).add(key);
        specByKey.put(key, spec);
    }

    public void removeActive(ResourceType rt, String key) {
        if (rt == null || key == null || key.isEmpty()) return;
        var set = activeByResource.get(rt);
        if (set != null) {
            set.remove(key);
            if (set.isEmpty()) activeByResource.remove(rt);
        }
        specByKey.remove(key);
        lastActivityTick.remove(key);
        lastDecayTick.remove(key);
        lastProgressIntSent.remove(key);
    }

    public Set<String> getActiveKeys(ResourceType rt) {
        var s = activeByResource.get(rt);
        return s == null ? java.util.Set.of() : java.util.Set.copyOf(s);
    }

    public SpendThresholdSpec getSpec(String key) {
        return specByKey.get(key);
    }
}
