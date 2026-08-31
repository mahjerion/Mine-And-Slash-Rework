package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Set;

public class DataDrivenSpendThresholdSpec extends SpendThresholdSpec {

    public enum ThresholdMode { FLAT, PERCENT_OF_MAX }

    private final ThresholdMode mode;
    private final float value;
    private final boolean multiplyByLevel;
    @Nullable private final ResourceType percentMaxOf;

    public DataDrivenSpendThresholdSpec(
            String key,
            ResourceType resource,
            ThresholdMode mode,
            float value,
            boolean multiplyByLevel,
            @Nullable ResourceType percentMaxOf,
            Set<String> lockWhileEffectIds,
            int cooldownTicks,
            boolean lockWhileCooldown,
            boolean dropProgressWhileLocked,
            boolean dropProgressOnProc,
            boolean showUi
    ) {
        super(resource, 0f, key,
              lockWhileEffectIds, cooldownTicks, lockWhileCooldown, dropProgressWhileLocked, dropProgressOnProc);
        this.mode = mode;
        this.value = value;
        this.multiplyByLevel = multiplyByLevel;
        this.percentMaxOf = percentMaxOf;
    }

    // Backward-compatible ctor (defaults showUi=false)
    public DataDrivenSpendThresholdSpec(
            String key,
            ResourceType resource,
            ThresholdMode mode,
            float value,
            boolean multiplyByLevel,
            @Nullable ResourceType percentMaxOf,
            Set<String> lockWhileEffectIds,
            int cooldownTicks,
            boolean lockWhileCooldown,
            boolean dropProgressWhileLocked,
            boolean dropProgressOnProc
    ) {
        this(key, resource, mode, value, multiplyByLevel, percentMaxOf, lockWhileEffectIds, cooldownTicks, lockWhileCooldown, dropProgressWhileLocked, dropProgressOnProc, false);
    }

    @Override
    public float thresholdFor(EntityData unit) {
        float base;
        switch (mode) {
            case PERCENT_OF_MAX -> {
                // default to this spec's resource if percentOf is null
                ResourceType tgt = (percentMaxOf != null) ? percentMaxOf : resource();
                float max = unit.getResources().getMax(unit.getEntity(), tgt);
                base = (value / 100f) * max;
            }
            case FLAT -> base = value;
            default -> base = value;
        }
        if (multiplyByLevel) base *= Math.max(1, unit.getLevel());
        return Math.max(0f, base);
    }

    @Override
    public void onProc(ServerPlayer sp, int procs) {
        // No default action here; datapack loader wires actions.
    }

}
