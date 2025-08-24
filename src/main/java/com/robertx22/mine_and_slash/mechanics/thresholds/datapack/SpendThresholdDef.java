package com.robertx22.mine_and_slash.mechanics.thresholds.datapack;

import com.google.gson.annotations.SerializedName;
import com.robertx22.mine_and_slash.mechanics.thresholds.DataDrivenSpendThresholdSpec;
import com.robertx22.mine_and_slash.mechanics.thresholds.SpendThresholdSpec;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class SpendThresholdDef {
    public String key;
    public String resource = "energy";
    public boolean enabled = true;
    public int priority = 0;

    public static class Threshold {
        public String mode = "X_PER_LEVEL";
        public float value = 0f;
        @SerializedName("multiply_by_level") public boolean multiplyByLevel = false;
        @SerializedName("pct_of") public String pctOf; // optional
    }
    public Threshold threshold = new Threshold();

    public static class Locks {
        public List<String> effects = new ArrayList<>();
        @SerializedName("lock_while_cooldown") public boolean lockWhileCooldown = true;
        @SerializedName("drop_progress_while_locked") public boolean dropProgressWhileLocked = true;
        @SerializedName("reset_progress_on_proc") public boolean resetProgressOnProc = true;
    }
    public Locks locks = new Locks();

    @SerializedName("cooldown_seconds")
    public int cooldownSeconds = 0;

    public static class ProcAction {
        public String action; // "apply_effect"
        @SerializedName("effect_id") public String effectId;
        @SerializedName("duration_ticks") public int durationTicks = 200;
        public int stacks = 1;
    }
    @SerializedName("on_proc")
    public List<ProcAction> onProc = new ArrayList<>();

    public SpendThresholdSpec toSpec() {
        ResourceType res = ResourceType.valueOf(resource.toLowerCase(java.util.Locale.ROOT));
        DataDrivenSpendThresholdSpec.ThresholdMode mode =
                DataDrivenSpendThresholdSpec.ThresholdMode.valueOf(threshold.mode.toUpperCase(Locale.ROOT));

        ResourceType pctOf = null;
        if ("PCT_OF_MAX".equalsIgnoreCase(threshold.mode) && threshold.pctOf != null && !threshold.pctOf.isEmpty()) {
            pctOf = ResourceType.valueOf(threshold.pctOf.toLowerCase(Locale.ROOT));
        }

        Set<String> lockEff = new HashSet<>(locks.effects);

        return new DataDrivenSpendThresholdSpec(
                key,
                res,
                mode,
                threshold.value,
                threshold.multiplyByLevel,
                pctOf,
                lockEff,
                SpendThresholdSpec.secondsToTicks(cooldownSeconds),
                locks.lockWhileCooldown,
                locks.dropProgressWhileLocked,
                locks.resetProgressOnProc
        ) {
            @Override
            public void onProc(ServerPlayer sp, int procs) {
                if (onProc == null || onProc.isEmpty()) return;

                var unit = com.robertx22.mine_and_slash.uncommon.datasaving.Load.Unit(sp);
                var store = unit.getStatusEffectsData();

                for (ProcAction a : onProc) {
                    if (!"apply_effect".equalsIgnoreCase(a.action) || a.effectId == null) continue;

                    var fx = com.robertx22.mine_and_slash.database.registry.ExileDB.ExileEffects().get(a.effectId);
                    if (fx == null) continue;

                    var inst = store.getOrCreate(fx);
                    int stacks = Math.max(1, a.stacks);
                    if (fx.max_stacks > 0) stacks = Math.min(stacks, fx.max_stacks);
                    inst.stacks = Math.max(inst.stacks, stacks);
                    inst.ticks_left = Math.max(inst.ticks_left, Math.max(1, a.durationTicks));

                    fx.onApply(sp);
                    unit.sync.setDirty();
                }
            }
        }.withPriority(priority);
    }
}
