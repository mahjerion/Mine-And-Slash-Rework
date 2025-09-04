package com.robertx22.mine_and_slash.mmorpg;

/**
 * Central debug HUD/flags. Toggle via commands or config as needed.
 * Keep flags conservative (default false) unless explicitly enabled.
 */
public final class DebugHud {

    private DebugHud() {}

    public static volatile boolean ON_EXPIRE = false;

    private static final java.util.concurrent.ConcurrentHashMap<String, Long> LAST_MSG_MS = new java.util.concurrent.ConcurrentHashMap<>();

    public static void send(net.minecraft.server.level.ServerPlayer sp, String key, String msg) {
        send(sp, key, msg, 500); // default 0.5s throttle per key
    }

    public static void send(net.minecraft.server.level.ServerPlayer sp, String key, String msg, int minIntervalMs) {
        if (sp == null) return;
        long now = System.currentTimeMillis();
        String k = sp.getUUID().toString() + ":" + key;
        Long last = LAST_MSG_MS.get(k);
        if (last != null && (now - last) < Math.max(0, minIntervalMs)) {
            return;
        }
        LAST_MSG_MS.put(k, now);
        sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
    }

}


