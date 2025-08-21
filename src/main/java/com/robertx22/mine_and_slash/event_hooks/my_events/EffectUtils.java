package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.mine_and_slash.aoe_data.database.stats.base.EffectCtx;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.server.level.ServerPlayer;

public final class EffectUtils {
    private EffectUtils() {}

    public static void applyState(ServerPlayer sp, EffectCtx ctx, int durationTicks, int stacks) {
        ExileEffect effect = ExileDB.ExileEffects().get(ctx.resourcePath);
        if (effect == null) effect = ExileDB.ExileEffects().get(ctx.id);
        if (effect == null) return;

        var unit  = Load.Unit(sp);
        var store = unit.getStatusEffectsData();
        var inst  = store.get(effect); // runtime instance (created if missing)

        // Stacks is a public field on this branch
        int wanted = Math.max(1, stacks);
        int capped = effect.max_stacks > 0 ? Math.min(wanted, effect.max_stacks) : wanted;
        inst.stacks = Math.max(inst.stacks, capped);


        inst.ticks_left = Math.max(inst.ticks_left, durationTicks);

        effect.onApply(sp);
        unit.sync.setDirty();
    }
}
