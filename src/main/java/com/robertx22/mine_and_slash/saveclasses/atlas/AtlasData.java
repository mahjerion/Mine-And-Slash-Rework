package com.robertx22.mine_and_slash.saveclasses.atlas;

import com.robertx22.library_of_exile.database.atlas.AtlasNode;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.mine_and_slash.database.registry.ExileDB;

import java.util.HashSet;
import java.util.Set;

public class AtlasData {

    public Set<String> unlockedNodes = new HashSet<>();
    public Set<String> completedNodes = new HashSet<>();

    private transient boolean initialized = false;

    // unlocks all starting_node=true nodes if they haven't been added yet.
    // re-runs safely if new starting nodes are added to a datapack later.
    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        for (AtlasNode node : LibDatabase.AtlasNodes().getList()) {
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
        unlockedNodes.addAll(ExileDB.AtlasNodeLayouts().getList().get(0).calcData.getConnectedIds(node.id));
        return true;
    }
}
