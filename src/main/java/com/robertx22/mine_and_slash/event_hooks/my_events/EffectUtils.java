package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.mine_and_slash.aoe_data.database.stats.base.EffectCtx;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffectInstanceData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Utility for applying short-TTL "state" effects (e.g., leeching_state) to players.
 *
 * Semantics:
 *  - Resolve the effect from {@link EffectCtx}.
 *  - Ensure a stored runtime instance exists (store.getOrCreate).
 *  - Refresh by taking MAX of existing vs. new stacks/ticks (never reduces).
 *  - Call effect.onApply(...) and mark the unit dirty for sync.
 *
 * Notes:
 *  - This variant is player-only by design. If you need NPCs/mobs later,
 *    add an overload for LivingEntity and only sync when the target is a ServerPlayer.
 *  - If resolve fails (null effect), we silently no-op; OnResourceRestore already
 *    provides a dev-only registry warning at the call site.
 */
public final class EffectUtils {
    private EffectUtils() {}

    public static ExileEffectInstanceData applyState(ServerPlayer sp, EffectCtx ctx, int durationTicks, int stacks) {
        final ExileEffect effect = resolveEffect(ctx);
        if (effect == null) return null;
        return applyEffect((LivingEntity) sp, effect, durationTicks, stacks);
    }

    public static ExileEffectInstanceData applyEffect(ServerPlayer sp, ExileEffect effect, int durationTicks, int stacks) {
        return applyEffect((LivingEntity) sp, effect, durationTicks, stacks, true);
    }

    public static ExileEffectInstanceData applyEffect(LivingEntity entity, ExileEffect effect, int durationTicks, int stacks) {
        return applyEffect(entity, effect, durationTicks, stacks, true);
    }

    public static ExileEffectInstanceData applyEffect(LivingEntity entity, ExileEffect effect, int durationTicks, int stacks, boolean markDirty) {
        if (effect == null || entity == null) return null;
        if (durationTicks <= 0 || stacks <= 0) return null;

        var unit  = Load.Unit(entity);
        var store = unit.getStatusEffectsData();
        var inst  = store.getOrCreate(effect);

        final int wanted = Math.max(1, stacks);
        final int capped = (effect.max_stacks > 0) ? Math.min(wanted, effect.max_stacks) : wanted;
        inst.stacks     = Math.max(inst.stacks, capped);
        inst.ticks_left = Math.max(inst.ticks_left, durationTicks);

        try { effect.onApply(entity); } catch (Exception ignored) {}
        if (markDirty) {
            unit.equipmentCache.STATUS.setDirty();
            if (entity instanceof ServerPlayer) {
                unit.sync.setDirty();
            }
        }
        return inst;
    }

    private static ExileEffect resolveEffect(EffectCtx ctx) {
        ExileEffect eff = ExileDB.ExileEffects().get(ctx.resourcePath);
        if (eff == null) eff = ExileDB.ExileEffects().get(ctx.id);
        return eff;
    }
}
