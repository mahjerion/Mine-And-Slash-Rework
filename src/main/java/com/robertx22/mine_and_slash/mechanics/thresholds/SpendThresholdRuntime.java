package com.robertx22.mine_and_slash.mechanics.thresholds;

import java.util.HashMap;
import java.util.Map;

public class SpendThresholdRuntime {
    // gameTime (ticks) when each key’s cooldown ends
    private final Map<String, Long> cooldownUntil = new HashMap<>();

    /** Start/refresh cooldown for a key. */
    public void startCooldown(String key, long now, int cooldownTicks) {
        if (cooldownTicks <= 0) return;
        cooldownUntil.put(key, now + cooldownTicks);
    }

    /** True if now is still before the stored end time. */
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

    /** Clear a key’s cooldown (optional utility). */
    public void clearCooldown(String key) {
        cooldownUntil.remove(key);
    }
}
