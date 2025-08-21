package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

public class LeechDebug {
    // Flip to false to silence without removing code (you can later wire this to a Forge config)
    public static boolean ENABLED = true;
    public static float MIN_TICK_AMOUNT = 0.01f; // or 1.0f if you prefer

    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    public static void tick(ServerPlayer p, ResourceType type, float amount) {
        if (!ENABLED) return;
        if (amount < MIN_TICK_AMOUNT) return; // avoid spam for tiny trickle

        // First tick -> announce START once
        if (ACTIVE.add(p.getUUID())) {
            // 'true' displays in the action bar; use 'false' for chat
             p.sendSystemMessage(net.minecraft.network.chat.Component.literal("[DEBUG] Leech START"));
        }

        // Print each second while leeching
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            String.format(java.util.Locale.US, "[DEBUG] Leech +%.1f %s", amount, type.name())
        ));

        // If you prefer console logs instead of action bar, uncomment:
        // SlashRef.LOGGER.info("[DEBUG] {} leech +{} {}", p.getGameProfile().getName(), amount, type.name());
    }

    public static void maybeStop(ServerPlayer p) {
        if (!ENABLED) return;
        if (ACTIVE.remove(p.getUUID())) {
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("[DEBUG] Leech STOP"));
        }
    }
}
