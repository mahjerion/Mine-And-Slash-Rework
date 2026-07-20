package com.robertx22.mine_and_slash.database.data.atlas;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.data.atlas.parser.AtlasGrid;
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

    public transient CalcData calcData = new CalcData();

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
        new AtlasGrid(this, nodes).loadIntoTree();
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
