package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects;
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

    // --- gameplay flags ---
    private static final int STATE_TICKS = 30; // short-lived; refresh while ticks keep coming

    // --- debug controls ---
    public static boolean DEBUG_ENABLED = true;
    public static float MIN_DEBUG_AMOUNT = 1.0f;
    private static final EnumSet<RestoreType> DEBUG_TYPES = EnumSet.of(RestoreType.leech); // default: only leech
    private static final Map<Key, Long> nextAllowedTick = new ConcurrentHashMap<>();
    private static final int PRINT_COOLDOWN_TICKS = 5; // 0.25s @20tps

    public static void trigger(LivingEntity entity,
                               ResourceType type,
                               float amount,
                               RestoreType restoreType) {

        // This hook should be called only after 'applied > 0'
        if (amount <= 0) return;

        // ===== 1) APPLY/REFRESH STATE FLAGS =====
        if (entity instanceof ServerPlayer sp) {
            switch (restoreType) {
                case leech -> {
                    // generic leech
                    EffectUtils.applyState(sp, ModEffects.LEECHING_STATE, STATE_TICKS, 1);
                    // per-resource leech
                    var fx = ModEffects.LEECHING_STATE_BY_RES.get(type);
                    if (fx != null) {
                        EffectUtils.applyState(sp, fx, STATE_TICKS, 1);
                    }
                }
                case regen -> {
                    // generic regen
                    EffectUtils.applyState(sp, ModEffects.REGEN_STATE, STATE_TICKS, 1);
                    // per-resource regen
                    var fx = ModEffects.REGEN_STATE_BY_RES.get(type);
                    if (fx != null) {
                        EffectUtils.applyState(sp, fx, STATE_TICKS, 1);
                    }
                }
                default -> {
                    // ignore other restore types for this feature
                }
            }
        }

        // ===== 2) OPTIONAL DEBUG PRINT =====
        if (!DEBUG_ENABLED) return;
        if (!(entity instanceof ServerPlayer sp)) return;
        if (!DEBUG_TYPES.contains(restoreType)) return;
        if (amount < MIN_DEBUG_AMOUNT) return;

        long now = sp.level().getGameTime();
        Key key = new Key(sp.getUUID(), type, restoreType);
        long allowedAt = nextAllowedTick.getOrDefault(key, 0L);
        if (now < allowedAt) return;

        String msg = String.format(Locale.US, "[RESTORE] +%.1f %s via %s",
                amount, type.name(), restoreType.name());
        sp.sendSystemMessage(Component.literal(msg));

        nextAllowedTick.put(key, now + PRINT_COOLDOWN_TICKS);
    }

    private record Key(UUID player, ResourceType type, RestoreType restoreType) {
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(player, k.player) && type == k.type && restoreType == k.restoreType;
        }
        @Override public int hashCode() { return Objects.hash(player, type, restoreType); }
    }
}



        