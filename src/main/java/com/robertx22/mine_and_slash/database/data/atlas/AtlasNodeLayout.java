package com.robertx22.mine_and_slash.database.data.atlas;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.data.atlas.parser.AtlasGrid;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.saveclasses.PointData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Single datapack entry holding the whole Atlas unlock map's layout as one 2d grid string,
// same format/authoring workflow as TalentTree.perks (see database/data/talent_tree/TalentTree.java) -
// a node's position is its row/column in the grid, and connections between nodes are computed
// automatically by AtlasGrid rather than being hand-declared. AtlasNode itself (Library-of-Exile-Rework)
// carries no position/connection fields at all.
public class AtlasNodeLayout implements JsonExileRegistry<AtlasNodeLayout>, IAutoGson<AtlasNodeLayout> {

    public static AtlasNodeLayout EMPTY = new AtlasNodeLayout();

    public static AtlasNodeLayout SERIALIZER = new AtlasNodeLayout();

    public String identifier = "unknown";

    // 2d grid with whitespace - see TalentTree.perks for the format
    public String nodes = "";

    // Built lazily by getCalcData(), NOT at json-load time - see onLoadedFromJson() below.
    private transient CalcData calcData = null;

    // The Atlas map is a single-entry registry, but that entry is hand-authored (shouldGenerateJson()
    // is false below), so a datapack that shadows or omits mmorpg_atlas_layout/atlas_map.json leaves
    // the registry empty. Every caller goes through here and gets EMPTY's blank CalcData - an empty
    // Atlas map renders as "nothing unlocked" instead of throwing IndexOutOfBounds, which on the
    // map-completion path would otherwise fire for every player on every cleared map.
    public static CalcData mainCalcData() {
        var list = ExileDB.AtlasNodeLayouts().getList();
        if (list.isEmpty()) {
            return EMPTY.getCalcData();
        }
        return list.get(0).getCalcData();
    }

    // Parsing the grid resolves every cell against the AtlasNode registry (see AtlasGridPoint), so it
    // can only run once that registry is fully populated. Doing it here, on first use, is the only
    // way to guarantee that on a client: the old eager parse in onLoadedFromJson() ran on the netty
    // decode thread while EfficientRegistryPacket was still decoding, whereas the AtlasNode entries
    // are only put into the registry by that packet's onReceived, which is deferred to the main
    // thread via enqueueWork. Cells that lost that race were never classified as nodes and the bad
    // result was cached for the session, so players saw a partly populated Atlas map until they
    // relogged (the client registry survives a disconnect, so the second parse succeeded).
    // Registry `order` cannot fix this - it orders the packet *sends*, not parse-vs-register.
    // synchronized because in singleplayer the client and integrated-server threads share this object.
    public synchronized CalcData getCalcData() {
        if (calcData != null) {
            return calcData;
        }
        CalcData built = new CalcData();
        if (!nodes.isBlank()) {
            try {
                new AtlasGrid(built, nodes).loadIntoTree();
            } catch (Exception e) {
                // cache the blank result rather than rethrowing - mainCalcData() is on the
                // map-completion path, so a broken layout must fail once and loudly instead of
                // throwing for every player on every cleared map
                ExileLog.get().warn("Failed to parse atlas layout '" + identifier + "', Atlas map will be empty.");
                e.printStackTrace();
                built = new CalcData();
            }
        }
        calcData = built;
        return calcData;
    }

    // called when the AtlasNode registry may have changed under us (datapack reload on the server,
    // login sync on the client) - see DatabaseCaches.resetCaches()
    public synchronized void invalidateCalcData() {
        this.calcData = null;
    }

    @Override
    public Class<AtlasNodeLayout> getClassForSerialization() {
        return AtlasNodeLayout.class;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.ATLAS_NODE_LAYOUT;
    }

    @Override
    public int Weight() {
        return 1000;
    }

    @Override
    public String GUID() {
        return identifier;
    }

    @Override
    public void onLoadedFromJson() {
        // deliberately does NOT parse the grid - this runs on the netty decode thread on clients,
        // before the AtlasNode registry it depends on is populated. getCalcData() does the parse.
        invalidateCalcData();
    }

    @Override
    public boolean shouldGenerateJson() {
        return false; // authored by hand/spreadsheet as a grid, same as TalentTree
    }

    public static class CalcData {

        public PointData center;

        public transient HashMap<PointData, Set<PointData>> connections = new HashMap<>();
        public transient HashMap<PointData, String> nodes = new HashMap<>();
        public transient HashMap<String, PointData> pointOf = new HashMap<>();

        public boolean isConnected(PointData one, PointData two) {
            if (!connections.containsKey(one)) {
                return false;
            }
            return connections.get(one).contains(two);
        }

        public void addNode(PointData point, String nodeId) {
            nodes.put(point, nodeId);
            pointOf.put(nodeId, point);
        }

        public void addConnection(PointData from, PointData to) {

            if (from.x == to.x && from.y == to.y) {
                return;
            }

            if (!connections.containsKey(from)) {
                connections.put(from, new HashSet<>());
            }
            if (!connections.containsKey(to)) {
                connections.put(to, new HashSet<>());
            }
            connections.get(from).add(to);
            connections.get(to).add(from);
        }

        // ids of the nodes directly connected to the given node id, per the grid - replaces the
        // old AtlasNode.neighbors list
        public Set<String> getConnectedIds(String nodeId) {
            PointData p = pointOf.get(nodeId);
            if (p == null || !connections.containsKey(p)) {
                return Set.of();
            }
            return connections.get(p).stream()
                    .map(nodes::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

    }

}
