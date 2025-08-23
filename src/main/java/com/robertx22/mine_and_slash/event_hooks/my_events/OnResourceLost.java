package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.mine_and_slash.mechanics.thresholds.SpendThresholdManager;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Unified entrypoint for resource LOSS (spend, drains, damage).
 * Health damage integration calls this via the LivingDamageEvent handler below.
 *
 * Debug printing is handled inside SpendThresholdManager and is toggled by
 * OnResourceLost.DEBUG_ENABLED.
 */
public final class OnResourceLost {
    private OnResourceLost() {}

    public enum LossSource { SpendOrDrain, Damage, Other }

    /** Toggle SpendThresholdManager debug logs per player. */
    public static boolean DEBUG_ENABLED = true;

    /** Call this whenever a resource actually goes down. */
    public static void trigger(LivingEntity entity, ResourceType type, float loss, LossSource source) {
        if (loss <= 0f) return;
        if (!(entity instanceof ServerPlayer sp)) return;

        var unit = Load.Unit(sp);
        long now = sp.level().getGameTime(); // ticks
        SpendThresholdManager.processSpend(sp, unit, type, loss, now);
    }

    /** Wire health damage into the unified loss path. */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onLivingDamage(net.minecraftforge.event.entity.living.LivingDamageEvent evt) {
        if (!(evt.getEntity() instanceof ServerPlayer sp)) return;
        float applied = evt.getAmount();
        if (applied > 0f) {
            trigger(sp, ResourceType.health, applied, LossSource.Damage);
        }
    }
}



