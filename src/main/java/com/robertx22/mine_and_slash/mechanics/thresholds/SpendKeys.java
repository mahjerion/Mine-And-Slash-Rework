package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.server.level.ServerPlayer;

public final class SpendKeys {
    private SpendKeys() {}
    public static String key(String nodeId, ResourceType rt) { return "spend." + rt.id + "." + nodeId; }
    public static float threshold(ServerPlayer sp, float perLevelFactor) {
        return perLevelFactor * Load.Unit(sp).getLevel();
    }
}
