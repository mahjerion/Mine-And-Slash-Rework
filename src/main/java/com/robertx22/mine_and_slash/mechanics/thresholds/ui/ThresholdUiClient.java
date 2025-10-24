package com.robertx22.mine_and_slash.mechanics.thresholds.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ThresholdUiClient {
    private ThresholdUiClient() {}

    private static final Map<String, String> keyToResource = new HashMap<>();
    private static final Map<String, Float> keyToProgress = new HashMap<>();

    public static void applyUpdate(String key, String resourceId, boolean show) {
        if (key == null || key.isEmpty()) return;
        if (show) {
            keyToResource.put(key, resourceId == null ? "" : resourceId);
        } else {
            keyToResource.remove(key);
            keyToProgress.remove(key);
        }
    }

    public static void setProgress(String key, float progress) {
        if (key == null || key.isEmpty()) return;
        keyToProgress.put(key, Math.max(0f, progress));
    }

    public static float getProgress(String key) {
        return keyToProgress.getOrDefault(key, 0f);
    }

    public static boolean isVisible(String key) {
        return keyToResource.containsKey(key);
    }

    public static Map<String, String> visibleEntries() {
        return Collections.unmodifiableMap(keyToResource);
    }
}


