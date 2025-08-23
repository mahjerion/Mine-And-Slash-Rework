package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.RestoreType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fires whenever a resource actually restored ("applied > 0").
 * Semantics are *literal*: we refresh short-TTL "state" flags only when ticks land.
 * - First hit does NOT benefit.
 * - Subsequent hits benefit while ticks are flowing (TTL bridges tick gaps).
 *
 * Extension points:
 *  1) Add a new RestoreType branch in {@link #onRestore(ServerPlayer, ResourceType, float, RestoreType)}.
 *  2) Keep any per-type registry sanity checks in a tiny ensure*Present(...) method.
 *  3) Apply/refresh states via EffectUtils.applyState(...) with a conservative TTL.
 */
public class OnResourceRestore {

    // ===== Gameplay tuning =====
    /** State lifetime in ticks; should exceed your leech cadence + jitter. */
    private static final int STATE_TICKS = 60; // ~3.0s @20tps

    // ===== Debug controls =====
    /** Global toggle for chat debug. Safe to leave false in prod. */
    public static boolean DEBUG_ENABLED = true;
    /** Ignore tiny restores in debug spam. */
    public static float MIN_DEBUG_AMOUNT = 1.0f;
    /** Which restore kinds print debug (default: leech only). */
    private static final EnumSet<RestoreType> DEBUG_TYPES = EnumSet.of(RestoreType.leech);
    /** Per (player, resource, type) cooldown for chat spam. */
    private static final Map<Key, Long> nextAllowedTick = new ConcurrentHashMap<>();
    private static final int PRINT_COOLDOWN_TICKS = 5; // 0.25s @20tps

    /** Public entrypoint from restore sites. Pass the entity that RECEIVED the restore (attacker for leech). */
    public static void trigger(LivingEntity entity,
                               ResourceType type,
                               float amount,
                               RestoreType restoreType) {
        // Must only be called when net-applied > 0
        if (amount <= 0) return;

        // 1) Apply/refresh state flags on ServerPlayer only (current design scope).
        if (entity instanceof ServerPlayer sp) {
            onRestore(sp, type, amount, restoreType);
        }

        // 2) Optional debug print (player-only)
        if (entity instanceof ServerPlayer sp) {
            maybeDebugRestore(sp, type, amount, restoreType);
        }
    }

    // ====== RestoreType routing (single-responsibility helpers below) ======

    private static void onRestore(ServerPlayer sp,
                                  ResourceType type,
                                  float amount,
                                  RestoreType restoreType) {
        switch (restoreType) {
            case leech -> applyLeechStates(sp, type);
            // === Add NEW RestoreType cases here ===
            // case <NEW_KIND> -> applyNewKindStates(sp, type, amount);
            default -> { /* ignore other kinds by default */ }
        }
    }

    /** Leech: refresh generic + per-resource flags on the SOURCE player. */
    private static void applyLeechStates(ServerPlayer sp, ResourceType type) {
        // Optional runtime sanity (helps catch missing datapack JSON in dev)
        if (!ensureLeechEffectsPresent(sp, type)) {
            return; // don’t pretend we applied anything
        }

        // Generic "while leeching"
        EffectUtils.applyState(sp, ModEffects.LEECHING_STATE, STATE_TICKS, 1);

        // Per-resource "while leeching [resource]"
        var fx = ModEffects.LEECHING_STATE_BY_RES.get(type);
        if (fx != null) {
            EffectUtils.applyState(sp, fx, STATE_TICKS, 1);
        }
    }

    /**
     * Datapack/registry guard. Returns true if both generic and per-resource
     * leech flags exist in the ExileDB registry.
     */
    private static boolean ensureLeechEffectsPresent(ServerPlayer sp, ResourceType type) {
        var anyFx   = ExileDB.ExileEffects().get(ModEffects.LEECHING_STATE.GUID());
        var byResFx = ExileDB.ExileEffects().get(ModEffects.LEECHING_STATE_BY_RES.get(type).GUID());

        if (anyFx != null && byResFx != null) return true;

        // Only nag in dev when debug is on; silence in prod.
        if (DEBUG_ENABLED) {
            sp.sendSystemMessage(Component.literal(
                "[RESTORE][WARN] Missing leech effects: any=" + (anyFx != null) +
                ", byRes=" + (byResFx != null) +
                " id(any)=" + ModEffects.LEECHING_STATE.GUID() +
                " id(byRes)=" + ModEffects.LEECHING_STATE_BY_RES.get(type).GUID()
            ));
        }
        return false;
    }

    /** Centralized debug; respects type filters & cooldown. */
    private static void maybeDebugRestore(ServerPlayer sp,
                                          ResourceType type,
                                          float amount,
                                          RestoreType restoreType) {
        if (!DEBUG_ENABLED) return;
        if (!DEBUG_TYPES.contains(restoreType)) return;
        if (amount < MIN_DEBUG_AMOUNT) return;

        long now = sp.level().getGameTime();
        Key key = new Key(sp.getUUID(), type, restoreType);
        long allowedAt = nextAllowedTick.getOrDefault(key, 0L);
        if (now < allowedAt) return;

        String msg = String.format(Locale.US,
            "[RESTORE] +%.1f %s via %s", amount, type.name(), restoreType.name());
        sp.sendSystemMessage(Component.literal(msg));
        nextAllowedTick.put(key, now + PRINT_COOLDOWN_TICKS);
    }

    // ====== internals ======

    private record Key(UUID player, ResourceType type, RestoreType restoreType) {
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(player, k.player) && type == k.type && restoreType == k.restoreType;
        }
        @Override public int hashCode() { return Objects.hash(player, type, restoreType); }
    }
}




        