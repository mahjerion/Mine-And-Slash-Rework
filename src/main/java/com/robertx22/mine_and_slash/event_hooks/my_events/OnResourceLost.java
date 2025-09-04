package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.mine_and_slash.mechanics.thresholds.SpendThresholdManager;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class OnResourceLost {
    private OnResourceLost() {}

    public enum LossSource { SpendOrDrain, Damage, Other }

    public static boolean DEBUG_ENABLED = false;

    public static void trigger(LivingEntity entity, ResourceType type, float loss, LossSource source) {
        if (loss <= 0f) return;
        if (!(entity instanceof ServerPlayer sp)) return;

        var unit = Load.Unit(sp);
        long now = sp.level().getGameTime(); // ticks
        SpendThresholdManager.processSpend(sp, unit, type, loss, now);
    }
}



