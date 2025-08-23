package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Set;

public class DataDrivenSpendThresholdSpec extends SpendThresholdSpec {

    public enum ThresholdMode { X_PER_LEVEL, FLAT, PCT_OF_MAX }

    private final ThresholdMode mode;
    private final float value;
    private final boolean multiplyByLevel;
    @Nullable private final ResourceType pctMaxOf;

    public DataDrivenSpendThresholdSpec(
            String key,
            ResourceType resource,
            ThresholdMode mode,
            float value,
            boolean multiplyByLevel,
            @Nullable ResourceType pctMaxOf,
            Set<String> lockWhileEffectIds,
            int cooldownTicks,
            boolean lockWhileCooldown,
            boolean dropProgressWhileLocked,
            boolean resetProgressOnProc
    ) {
        super(resource, /*perLevelFactor (unused)*/ 0f, key,
              lockWhileEffectIds, cooldownTicks, lockWhileCooldown, dropProgressWhileLocked, resetProgressOnProc);
        this.mode = mode;
        this.value = value;
        this.multiplyByLevel = multiplyByLevel;
        this.pctMaxOf = pctMaxOf;
    }

    @Override
    public float thresholdFor(EntityData unit) {
        float base;
        switch (mode) {
            case X_PER_LEVEL:
                base = value * Math.max(1, unit.getLevel());
                break;
            case FLAT:
                base = value * (multiplyByLevel ? Math.max(1, unit.getLevel()) : 1f);
                break;
            case PCT_OF_MAX:
                ResourceType rt = (pctMaxOf != null) ? pctMaxOf : resource();
                float max = unit.getResources().getMax(unit.getEntity(), rt);
                base = (value / 100f) * max;
                if (multiplyByLevel) base *= Math.max(1, unit.getLevel());
                break;
            default:
                base = 0f;
        }
        return Math.max(0f, base);
    }

    @Override
    public void onProc(ServerPlayer sp, int procs) {
        // No default action here; datapack loader wires actions.
    }
}
