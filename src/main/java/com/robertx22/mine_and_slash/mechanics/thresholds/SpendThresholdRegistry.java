package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;

import java.util.*;

public final class SpendThresholdRegistry {
    private SpendThresholdRegistry() {}

    private static final Map<ResourceType, List<SpendThresholdSpec>> BY_RES = new EnumMap<>(ResourceType.class);
    private static boolean FROZEN = false;
    private static int COUNT = 0;

    public static void clearAll() {
        BY_RES.clear();
        FROZEN = false;
        COUNT = 0;
    }

    public static void registerGlobal(SpendThresholdSpec spec) { registerGlobal(spec, 0); }

    public static void registerGlobal(SpendThresholdSpec spec, int priority) {
        if (spec == null || FROZEN) return;
        spec.withPriority(priority);
        BY_RES.computeIfAbsent(spec.resource(), __ -> new ArrayList<>()).add(spec);
        COUNT++;
    }

    /** Returns a copy sorted by priority (low first). */
    public static List<SpendThresholdSpec> resolveFor(EntityData unit, ResourceType rt) {
        var list = BY_RES.get(rt);
        if (list == null || list.isEmpty()) return List.of();
        var copy = new ArrayList<>(list);
        copy.sort(Comparator.comparingInt(SpendThresholdSpec::priority));
        return copy;
    }

    public static void freeze() { FROZEN = true; }
    public static int size() { return COUNT; }
}
