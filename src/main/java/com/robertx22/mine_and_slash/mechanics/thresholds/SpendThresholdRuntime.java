package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpendThresholdRuntime {

    private static final class KeyState {
        long cooldownUntil;
        long lastActivityTick;
        long lastDecayTick;
        int lastProgressIntSent = Integer.MIN_VALUE;
        SpendThresholdSpec spec;
    }

    private final Map<String, KeyState> states = new HashMap<>();

    private final java.util.EnumMap<ResourceType, Set<String>> activeByResource = new java.util.EnumMap<>(ResourceType.class);
    private final java.util.EnumMap<ResourceType, Set<String>> activeByResourceReadOnly = new java.util.EnumMap<>(ResourceType.class);

    public void startCooldown(String key, long now, int cooldownTicks) {
        if (cooldownTicks <= 0) return;
        if (key == null || key.isEmpty()) return;
        KeyState ks = states.computeIfAbsent(key, __ -> new KeyState());
        ks.cooldownUntil = now + cooldownTicks;
    }

    public boolean isCoolingDown(String key, long now) {
        if (key == null || key.isEmpty()) return false;
        KeyState ks = states.get(key);
        return ks != null && now < ks.cooldownUntil;
    }

    public int cooldownRemainingTicks(String key, long now) {
        if (key == null || key.isEmpty()) return 0;
        KeyState ks = states.get(key);
        long until = (ks == null) ? 0L : ks.cooldownUntil;
        long rem = until - now;
        return (int) Math.max(0, rem);
    }

    // === Activity/Decay tracking ===
    public void markActivity(String key, long now) {
        if (key == null || key.isEmpty()) return;
        KeyState ks = states.computeIfAbsent(key, __ -> new KeyState());
        ks.lastActivityTick = now;
        ks.lastDecayTick = 0L;
    }

    public long getLastActivity(String key) {
        if (key == null || key.isEmpty()) return 0L;
        KeyState ks = states.get(key);
        return ks == null ? 0L : ks.lastActivityTick;
    }

    public long getLastDecay(String key) {
        if (key == null || key.isEmpty()) return 0L;
        KeyState ks = states.get(key);
        return ks == null ? 0L : ks.lastDecayTick;
    }

    public void markDecay(String key, long now) {
        if (key == null || key.isEmpty()) return;
        KeyState ks = states.computeIfAbsent(key, __ -> new KeyState());
        ks.lastDecayTick = now;
    }

    public boolean progressIntChanged(String key, int intProgress) {
        if (key == null || key.isEmpty()) return false;
        KeyState ks = states.get(key);
        int prev = (ks == null) ? Integer.MIN_VALUE : ks.lastProgressIntSent;
        if (prev != intProgress) {
            if (ks == null) ks = states.computeIfAbsent(key, __ -> new KeyState());
            ks.lastProgressIntSent = intProgress;
            return true;
        }
        return false;
    }

    // === Active key index ===
    public void markActive(ResourceType rt, String key, SpendThresholdSpec spec) {
        if (rt == null || key == null || key.isEmpty() || spec == null) return;
        Set<String> set = activeByResource.get(rt);
        if (set == null) {
            set = new HashSet<>();
            activeByResource.put(rt, set);
            // create and cache a read-only view for this resource set to avoid future allocations
            activeByResourceReadOnly.put(rt, java.util.Collections.unmodifiableSet(set));
        }
        set.add(key);
        KeyState ks = states.computeIfAbsent(key, __ -> new KeyState());
        ks.spec = spec;
    }

    public void removeActive(ResourceType rt, String key) {
        if (rt == null || key == null || key.isEmpty()) return;
        var set = activeByResource.get(rt);
        if (set != null) {
            set.remove(key);
            if (set.isEmpty()) {
                activeByResource.remove(rt);
                activeByResourceReadOnly.remove(rt);
            }
        }
        KeyState ks = states.get(key);
        if (ks != null) {
            // clear volatile state but keep cooldown to preserve gating behavior
            ks.spec = null;
            ks.lastActivityTick = 0L;
            ks.lastDecayTick = 0L;
            ks.lastProgressIntSent = Integer.MIN_VALUE;
        }
    }

    public Set<String> getActiveKeys(ResourceType rt) {
        var s = activeByResource.get(rt);
        if (s == null || s.isEmpty()) return java.util.Set.of();
        var view = activeByResourceReadOnly.get(rt);
        if (view == null) {
            view = java.util.Collections.unmodifiableSet(s);
            activeByResourceReadOnly.put(rt, view);
        }
        return view;
    }

    public SpendThresholdSpec getSpec(String key) {
        if (key == null || key.isEmpty()) return null;
        KeyState ks = states.get(key);
        return ks == null ? null : ks.spec;
    }
}
