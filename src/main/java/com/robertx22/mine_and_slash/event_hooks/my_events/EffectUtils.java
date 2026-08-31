package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.mine_and_slash.aoe_data.database.stats.base.EffectCtx;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffectInstanceData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.server.level.ServerPlayer;

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

    /**
     * Apply/refresh a state effect on the player.
     *
     * @param sp            target player
     * @param ctx           effect context (ids defined in ModEffects)
     * @param durationTicks desired remaining lifetime (ticks); merged via MAX
     * @param stacks        desired stacks; clamped to effect.max_stacks and merged via MAX
     * @return ExileEffectInstanceData for the applied effect, or null if resolve failed.
     */
    public static ExileEffectInstanceData applyState(ServerPlayer sp, EffectCtx ctx, int durationTicks, int stacks) {
        final ExileEffect effect = resolveEffect(ctx);
        if (effect == null) return null;

        return applyEffect(sp, effect, durationTicks, stacks);
    }

    public static ExileEffectInstanceData applyEffect(ServerPlayer sp, ExileEffect effect, int durationTicks, int stacks) {
        if (effect == null) return null;

        var unit  = Load.Unit(sp);
        var store = unit.getStatusEffectsData();
        var inst  = store.getOrCreate(effect); // persist if missing

        // Merge stacks/ticks: refresh semantics (never decrease on re-apply)
        final int wanted = Math.max(1, stacks);
        final int capped = (effect.max_stacks > 0) ? Math.min(wanted, effect.max_stacks) : wanted;
        inst.stacks     = Math.max(inst.stacks, capped);
        inst.ticks_left = Math.max(inst.ticks_left, durationTicks);

        // Keep vanilla stats / one-of-a-kind cleanup in sync
        effect.onApply(sp);
        unit.sync.setDirty(); // network/state sync
        return inst;
    }

    /** Try both resourcePath (preferred) and id; some data uses either. */
    private static ExileEffect resolveEffect(EffectCtx ctx) {
        ExileEffect eff = ExileDB.ExileEffects().get(ctx.resourcePath);
        if (eff == null) eff = ExileDB.ExileEffects().get(ctx.id);
        return eff;
    }
}
