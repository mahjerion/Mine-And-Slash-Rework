package com.robertx22.mine_and_slash.saveclasses.atlas;

import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.database.atlas.AtlasNode;
import com.robertx22.mine_and_slash.database.data.atlas.AtlasNodeLayout;

import java.util.HashSet;
import java.util.Set;

public class AtlasData {

    public Set<String> unlockedNodes = new HashSet<>();
    public Set<String> completedNodes = new HashSet<>();
    public boolean pinnacleUnlocked = false;

    private transient boolean initialized = false;

    // unlocks all starting_node=true nodes if they haven't been added yet.
    // re-runs safely if new starting nodes are added to a datapack later.
    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        var nodes = DungeonDatabase.AtlasNodes().getList();
        // Do NOT latch on an empty registry. `initialized` is transient, so on a dedicated server it
        // starts false after this capability is deserialized on login - and login can run before the
        // atlas node registry is populated. Latching there left the player with zero unlocked nodes
        // for the whole session, so ON_MAP_FULLY_CLEARED's `if (!isUnlocked(node.id)) continue` gate
        // silently skipped every node and completing a map credited nothing. Singleplayer never hit
        // this because the integrated server already has the registry loaded.
        if (nodes.isEmpty()) {
            return;
        }
        initialized = true;
        for (AtlasNode node : nodes) {
            if (node.starting_node) {
                unlockedNodes.add(node.id);
            }
        }
    }

    public boolean isUnlocked(String nodeId) {
        ensureInitialized();
        return unlockedNodes.contains(nodeId);
    }

    public boolean isCompleted(String nodeId) {
        return completedNodes.contains(nodeId);
    }

    // returns true if this was a new completion (caller should grant points / sync)
    public boolean markCompleted(AtlasNode node) {
        ensureInitialized();
        if (!completedNodes.add(node.id)) {
            return false;
        }
        unlockedNodes.add(node.id);
        unlockedNodes.addAll(AtlasNodeLayout.mainCalcData().getConnectedIds(node.id));
        return true;
    }
}
