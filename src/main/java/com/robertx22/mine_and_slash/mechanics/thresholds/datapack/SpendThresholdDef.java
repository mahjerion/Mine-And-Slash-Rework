package com.robertx22.mine_and_slash.mechanics.thresholds.datapack;

import com.google.gson.annotations.SerializedName;
import com.robertx22.mine_and_slash.mechanics.thresholds.DataDrivenSpendThresholdSpec;
import com.robertx22.mine_and_slash.mechanics.thresholds.SpendThresholdSpec;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import net.minecraft.server.level.ServerPlayer;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.event_hooks.my_events.EffectUtils;

import java.util.*;

public class SpendThresholdDef {
    public String key;
    public String resource = "";
    public boolean enabled = true;
    public int priority = 0;
    @SerializedName("show_ui")
    public boolean showUi = false;

    public static class Threshold {
        public String mode = "FLAT";
        public float value = 0f;
        @SerializedName("multiply_by_level") public boolean multiplyByLevel = false;
        @SerializedName("percent_of") public String percentOf; // optional
    }
    public Threshold threshold = new Threshold();

    public static class Locks {
        public List<String> effects = new ArrayList<>();
        @SerializedName("lock_while_cooldown") public boolean lockWhileCooldown = false;
        @SerializedName("drop_progress_while_locked") public boolean dropProgressWhileLocked = true;
        @SerializedName("drop_progress_on_proc") public boolean dropProgressOnProc = true;
    }
    public Locks locks = new Locks();

    @SerializedName("cooldown_ticks")
    public int cooldownTicks = 0;

    @SerializedName("require_stat")
    public String requireStatId = "";

    public static class ProcAction {
        public String action; // "apply_effect"
        @SerializedName("exile_potion_id") public String effectId;
        @SerializedName("duration_ticks") public int durationTicks = 0;
        public int stacks = 1;
        @SerializedName("on_expire") public java.util.Map<String, Integer> onExpire = java.util.Collections.emptyMap();
    }
    @SerializedName("on_proc")
    public List<ProcAction> onProc = new ArrayList<>();

    public SpendThresholdSpec toSpec() {
        ResourceType res = parseResource(resource, ResourceType.energy);

        // Modes supported: FLAT (optionally with multiply_by_level) or PERCENT_OF_MAX
        String rawMode = (threshold.mode == null ? "FLAT" : threshold.mode.trim()).toUpperCase(Locale.ROOT);
        boolean mult = threshold.multiplyByLevel;
        DataDrivenSpendThresholdSpec.ThresholdMode mode =
                "PERCENT_OF_MAX".equals(rawMode)
                        ? DataDrivenSpendThresholdSpec.ThresholdMode.PERCENT_OF_MAX
                        : DataDrivenSpendThresholdSpec.ThresholdMode.FLAT; // default + treats legacy values as FLAT

        ResourceType percentOf = null;
        if (mode == DataDrivenSpendThresholdSpec.ThresholdMode.PERCENT_OF_MAX
                && threshold.percentOf != null && !threshold.percentOf.isEmpty()) {
            percentOf = parseResource(threshold.percentOf, res); // default to this spec’s resource if bad input
        }

        Set<String> lockEff = (locks != null && locks.effects != null)
                ? new HashSet<>(locks.effects) : Collections.emptySet();

        return new DataDrivenSpendThresholdSpec(
                key,
                res,
                mode,
                threshold.value,
                mult,
                percentOf,
                lockEff,
                cooldownTicks,
                locks != null && locks.lockWhileCooldown,
                locks != null && locks.dropProgressWhileLocked,
                locks != null && locks.dropProgressOnProc,
                showUi
        ) {
            @Override
            public void onProc(ServerPlayer sp, int procs) {
                if (onProc == null || onProc.isEmpty()) return;

                var unit  = com.robertx22.mine_and_slash.uncommon.datasaving.Load.Unit(sp);
                var store = unit.getStatusEffectsData();

                for (ProcAction a : onProc) {
                    if (!"exile_effect".equalsIgnoreCase(a.action) || a.effectId == null) continue;
                    var effect = ExileDB.ExileEffects().get(a.effectId);
                    if (effect == null) continue;

                    int durTicks = Math.max(1, a.durationTicks);
                    int stacks = Math.max(1, a.stacks);
                    var inst = EffectUtils.applyEffect(sp, effect, durTicks, stacks);
                }
            }

            @Override
            public boolean isLockedFor(EntityData unit) {
                if (super.isEffectLocked(unit)) return true;
                if (requireStatId != null && !requireStatId.isEmpty()) {
                    var st = ExileDB.Stats().get(requireStatId);
                    if (st != null) {
                        return unit.getUnit().getCalculatedStat(st).getValue() <= 0;
                    }
                }
                return false;
            }
        }.withPriority(priority);
    }

    // --- helpers ---
    private static ResourceType parseResource(String s, ResourceType fallback) {
        if (s == null) return fallback;
        for (ResourceType rt : ResourceType.values()) {
            if (rt.name().equalsIgnoreCase(s)) return rt;
            try {
                // if your enum exposes an id/string, handle it here:
                var idField = rt.getClass().getField("id");
                Object idVal = idField.get(rt);
                if (idVal instanceof String && ((String) idVal).equalsIgnoreCase(s)) return rt;
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }
        return fallback;
    }
}
