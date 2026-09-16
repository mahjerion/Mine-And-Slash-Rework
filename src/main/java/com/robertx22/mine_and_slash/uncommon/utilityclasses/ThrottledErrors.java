package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.library_of_exile.main.ExileLog;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Prints a recurring failure at most once every {@link #INTERVAL_MS} per key.
 * <p>
 * The per tick code paths that used to swallow an exception and quietly drop the rest of their work
 * now keep going and retry instead, which is what we want - but it also means a permanent failure
 * would print a stack trace twenty times a second per entity, which on a busy server is its own
 * outage. One trace every few seconds is enough to diagnose with, and it still makes a repeating
 * failure obvious rather than a one off.
 */
public class ThrottledErrors {

    private static final long INTERVAL_MS = 5000;

    private static final ConcurrentHashMap<String, Long> LAST_LOGGED = new ConcurrentHashMap<>();

    public static void log(String key, String message, Throwable error) {

        long now = System.currentTimeMillis();
        Long last = LAST_LOGGED.get(key);

        if (last != null && now - last < INTERVAL_MS) {
            return;
        }
        LAST_LOGGED.put(key, now);

        ExileLog.get().warn(message);
        error.printStackTrace();
    }
}
