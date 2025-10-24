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


public class OnResourceRestore {

    private static final int STATE_TICKS = 20;

    public static boolean DEBUG_ENABLED = false;

    public static float MIN_DEBUG_AMOUNT = 1.0f;

    private static final EnumSet<RestoreType> DEBUG_TYPES = EnumSet.of(RestoreType.leech);

    private static final Map<Key, Long> nextAllowedTick = new ConcurrentHashMap<>();
    private static final int PRINT_COOLDOWN_TICKS = 5; // 0.25s @20tps


    public static void trigger(LivingEntity entity,
                               ResourceType type,
                               float amount,
                               RestoreType restoreType) {
        if (amount <= 0) return;

        if (entity instanceof ServerPlayer sp) {
            onRestore(sp, type, amount, restoreType);
        }

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
            default -> {}
        }
    }

    private static void applyLeechStates(ServerPlayer sp, ResourceType type) {
        if (!ensureLeechEffectsPresent(sp, type)) {
            return;
        }

        EffectUtils.applyState(sp, ModEffects.LEECHING_STATE, STATE_TICKS, 1);

        var fx = ModEffects.LEECHING_STATE_BY_RES.get(type);
        if (fx != null) {
            EffectUtils.applyState(sp, fx, STATE_TICKS, 1);
        }
    }

    private static boolean ensureLeechEffectsPresent(ServerPlayer sp, ResourceType type) {
        var anyFx   = ExileDB.ExileEffects().get(ModEffects.LEECHING_STATE.GUID());
        var byResFx = ExileDB.ExileEffects().get(ModEffects.LEECHING_STATE_BY_RES.get(type).GUID());

        if (anyFx != null && byResFx != null) return true;

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




        