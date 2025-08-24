package com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition;

import com.robertx22.mine_and_slash.aoe_data.database.stats.base.EffectCtx;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Datapack-serializable condition that returns true iff the chosen side
 * (Source/Target) currently has a given {@link ExileEffect} active.
 *
 * Usage in code:
 *   .setSide(EffectSides.Source)
 *   .addCondition(new HasExileEffectCondition(ModEffects.LEECHING_STATE)) // e.g. "while leeching"
 *
 * Usage in datapack JSON (shape depends on your builder/serializer wiring):
 *   {
 *     "type": "has_exile_effect",
 *     "effectId": "leeching_state"
 *   }
 *
 * Notes:
 * - We rely on the store's real state via `has(fx)` (correct now that apply uses getOrCreate()).
 * - Keep this condition lightweight: just resolve the effect and query the status store.
 * - Debug can be toggled globally via OnResourceRestore.DEBUG_ENABLED (no spam by default).
 */
public class HasExileEffectCondition extends StatCondition {

    /** Serialized id of the effect to check (e.g., "leeching_state"). */
    public String effectId = "";

    /** Stable serializer id (must match your registry/serializer entry). */
    private static final String SER_ID = "has_exile_effect";

    /** Convenience ctor for code paths (accepts an EffectCtx like ModEffects.LEECHING_STATE). */
    public HasExileEffectCondition(EffectCtx ctx) {
        super(SER_ID + "_" + ctx.resourcePath, SER_ID);
        this.effectId = ctx.resourcePath;
    }

    /** No-arg ctor for (de)serialization. */
    public HasExileEffectCondition() {
        super("", SER_ID);
    }

    @Override
    public boolean can(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {
        if (effectId == null || effectId.isEmpty()) return false;

        // Resolve the effect definition
        final ExileEffect fx = ExileDB.ExileEffects().get(effectId);
        if (fx == null) return false;

        // Pick the entity to check based on the stat's side (match the stat!)
        final LivingEntity who = (statSource == EffectSides.Source) ? event.source : event.target;
        if (who == null) return false;

        // Read live status (no allocations): true iff stored instance exists AND is not removed
        final var effData = Load.Unit(who).getStatusEffectsData();
        final boolean has = (effData != null) && effData.has(fx);

        // Optional lightweight debug (off by default)
        if (com.robertx22.mine_and_slash.event_hooks.my_events.OnResourceRestore.DEBUG_ENABLED
                && who instanceof ServerPlayer sp) {
            final String side = (statSource == EffectSides.Source ? "SRC" : "TGT");
            final String flags = compactLeechFlags(effData); // e.g. "AMH" or "-"
            sp.sendSystemMessage(Component.literal(
                "[HasExileEffect] " + side +
                " id=" + effectId +
                " has=" + (has ? "Y" : "X") +
                " flags=" + (flags.isEmpty() ? "-" : flags)
            ));
        }

        return has;
    }

    // ----- helpers (class scope) -----

    private static String compactLeechFlags(
            com.robertx22.mine_and_slash.vanilla_mc.potion_effects.EntityStatusEffectsData effData) {
        if (effData == null) return "";

        StringBuilder b = new StringBuilder();

        if (hasFx(effData, com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects.LEECHING_STATE.GUID()))
            b.append('A'); // Any leech

        if (hasFx(effData, com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects
                .LEECHING_STATE_BY_RES.get(ResourceType.health).GUID()))
            b.append('H');

        if (hasFx(effData, com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects
                .LEECHING_STATE_BY_RES.get(ResourceType.mana).GUID()))
            b.append('M');

        if (hasFx(effData, com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects
                .LEECHING_STATE_BY_RES.get(ResourceType.energy).GUID()))
            b.append('E');

        if (hasFx(effData, com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects
                .LEECHING_STATE_BY_RES.get(ResourceType.magic_shield).GUID()))
            b.append('S');

        if (hasFx(effData, com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects
                .LEECHING_STATE_BY_RES.get(ResourceType.blood).GUID()))
            b.append('B');

        return b.toString();
    }

    private static boolean hasFx(
            com.robertx22.mine_and_slash.vanilla_mc.potion_effects.EntityStatusEffectsData effData,
            String effectId) {
        var fx = ExileDB.ExileEffects().get(effectId);
        return fx != null && effData.has(fx);
    }

    @Override
    public Class<? extends StatCondition> getSerClass() {
        return HasExileEffectCondition.class;
    }
}


