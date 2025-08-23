package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.event_hooks.my_events.OnResourceLost;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import net.minecraft.server.level.ServerPlayer;

/** Registers global spend thresholds at startup. */
public final class SpendThresholdManager {
    private SpendThresholdManager() {}

    public static void registerDefaults() {

        // 30 × LVL Energy, 10s cooldown, locked while effects are active
        SpendThresholdRegistry.registerGlobal(
            new DataDrivenSpendThresholdSpec(
                /* key            */ "ENERGY_XLVL_WRATH",
                /* resource       */ ResourceType.energy,
                /* mode           */ DataDrivenSpendThresholdSpec.ThresholdMode.X_PER_LEVEL,
                /* value          */ 30f,          // "X" in X per level
                /* multiplyByLvl  */ true,
                /* pctMaxOf       */ null,         // not used for X_PER_LEVEL
                /* lock effects   */ java.util.Set.of("wrath_of_the_juggernaut", "Burnout"),
                /* cooldown ticks */ SpendThresholdSpec.secondsToTicks(10),
                /* lockIfCooldown */ true,
                /* dropWhileLock  */ true,
                /* resetOnProc    */ true
            ) {
                @Override public void onProc(ServerPlayer sp, int procs) {
                    // TODO: apply Wrath here
                }
            }
            .withPriority(0)
        );

        // 30 × LVL Mana (example)
        SpendThresholdRegistry.registerGlobal(
            new DataDrivenSpendThresholdSpec(
                "MANA_XLVL",
                ResourceType.mana,
                DataDrivenSpendThresholdSpec.ThresholdMode.X_PER_LEVEL,
                30f,
                true,     // multiply by level
                null,
                java.util.Set.of(),    // no lock effects
                SpendThresholdSpec.secondsToTicks(10),
                true,      // lock while cooldown
                true,      // drop progress while locked
                true       // reset on proc
            ) {
                @Override public void onProc(ServerPlayer sp, int procs) {
                    // TODO: apply your mana proc effect here
                }
            }
           .withPriority(0)
        );

        // Example: Took 20% of Max Health (percent-of-max mode)
        SpendThresholdRegistry.registerGlobal(
            new DataDrivenSpendThresholdSpec(
                "TOOK_20PCT_HEALTH",
                ResourceType.health,
                DataDrivenSpendThresholdSpec.ThresholdMode.PCT_OF_MAX,
                20f,     // 20%
                false,   // multiplyByLevel (usually false for %)
                ResourceType.health, // pct-of which pool
                java.util.Set.of(),      // no lock effects by default
                0,        // no cooldown by default (tune as you like)
                false,    // don't treat cooldown as lock (since 0)
                true,     // drop progress if ever locked
                true      // reset on proc
            ) {
                @Override public void onProc(ServerPlayer sp, int procs) {
                    // TODO: apply your “took X% max life” effect here
                }
            }
            .withPriority(0)
        );
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
                    long rem = unit.getSpendRuntime().cooldownRemainingTicks(key, now); // ensure this exists
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[SPEND:" + spec.key() + "] locked by cooldown (" + rem + "t ~ " + fmtSec(rem) + "s)"
                    ));
                }
                continue;
            }

            // Effect lock
            if (spec.isEffectLocked(unit)) {
                if (spec.dropProgressWhileLocked()) {
                    tracker.clearKey(type, key);
                }
                if (debug) {
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[SPEND:" + spec.key() + "] locked by active effect(s)"
                    ));
                }
                continue;
            }


            float threshold = spec.thresholdFor(unit);
            if (threshold <= 0f) continue;

            int procs = tracker.addAndConsumeForKey(key, type, loss, threshold);
            if (procs > 0) {
                spec.onProc(sp, procs);
                spec.startCooldown(unit, now);
                if (spec.resetOnProc()) {
                    tracker.clearKey(type, key);
                }
                if (debug) dbg(sp, "[SPEND:" + spec.key() + "] " + type.id + " ×" + procs + " (thr=" + fmt(threshold) + ")");
            } else if (debug) {
                float cur = tracker.getKeyProgress(key, type);
                dbg(sp, "[SPEND:" + spec.key() + "] +" + fmt(loss) + " " + type.id + " (cur=" + fmt(cur) + " / " + fmt(threshold) + ")");
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
