package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.event_hooks.my_events.OnResourceLost;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import net.minecraft.server.level.ServerPlayer;

/** Registers global spend thresholds at startup. */
public final class SpendThresholdManager {
    private SpendThresholdManager() {}

    public static void registerDefaults() {
        // No-op: thresholds are defined via datapack JSON.
    }

    // ===== DEBUGGING =====
    public static void processSpend(ServerPlayer sp, EntityData unit, ResourceType type, float loss, long now) {
        processSpend(sp, unit, type, loss, now, OnResourceLost.DEBUG_ENABLED);
    }

    public static void processSpend(ServerPlayer sp, EntityData unit, ResourceType type, float loss, long now, boolean debug) {
        if (loss <= 0f) return;

        var tracker = unit.getResourceTracker();
        tracker.addLoss(type, loss); // general counter (optional)

        var specs = SpendThresholdRegistry.resolveFor(unit, type);
        if (specs.isEmpty()) {
            if (debug) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "[SPEND] +" + String.format(java.util.Locale.US, "%.1f", loss) +
                    " " + type.id + " (no specs)"
                ));
            }
            return;
        }

        for (SpendThresholdSpec spec : specs) {
            final String key = spec.keyFor(unit);

            // Cooldown-as-lock
            if (spec.lockWhileCooldown() && unit.getSpendRuntime().isCoolingDown(key, now)) {
                if (spec.dropProgressWhileLocked()) {
                    tracker.clearKey(type, key);
                }
                if (debug) {
                    long rem = unit.getSpendRuntime().cooldownRemainingTicks(key, now);
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[SPEND:" + spec.key() + "] locked by cooldown (" + rem + "t ~ " + fmtSec((float) rem) + "s)"
                    ));
                }
                continue;
            }

            // Locks (effects + optional perk requirement)
            if (spec.isLockedFor(unit)) {
                if (spec.dropProgressWhileLocked()) {
                    tracker.clearKey(type, key);
                }
                if (debug) {
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("[SPEND:" + spec.key() + "] locked"));
                }
                continue;
            }


            float threshold = spec.thresholdFor(unit);
            if (threshold <= 0f) continue;

            int procs = tracker.addAndConsumeForKey(key, type, loss, threshold);
            if (loss > 0f && procs == 0) {
                unit.getSpendRuntime().markActivity(key, now);
                unit.getSpendRuntime().markActive(type, key, spec);
            }
            if (procs > 0) {
                spec.onProc(sp, procs);
                spec.startCooldown(unit, now);
                if (spec.dropProgressOnProc()) {
                    tracker.clearKey(type, key);
                }
                if (debug) dbg(sp, "[SPEND:" + spec.key() + "] " + type.id + " ×" + procs + " (thr=" + fmt(threshold) + ")");
                unit.getSpendRuntime().removeActive(type, key);
            } else {
                float cur = tracker.getKeyProgress(key, type);
                if (debug) {
                    dbg(sp, "[SPEND:" + spec.key() + "] +" + fmt(loss) + " " + type.id + " (cur=" + fmt(cur) + " / " + fmt(threshold) + ")");
                }
                if (cur <= 0f) {
                    unit.getSpendRuntime().removeActive(type, key);
                }
            }
        }
    }

    // --- helpers ---
    private static void dbg(ServerPlayer sp, String msg) {
        if (!OnResourceLost.DEBUG_ENABLED) return;
        sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
    }
    private static String fmt(float v) { return String.format(java.util.Locale.US, "%.1f", v); }
    private static String fmtSec(float s) { return String.format(java.util.Locale.US, "%.1f", s); }
}
