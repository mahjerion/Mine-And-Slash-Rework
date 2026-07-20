package com.robertx22.mine_and_slash.database.data.atlas.parser;

import com.robertx22.mine_and_slash.database.data.atlas.AtlasNodeLayout;

import java.util.*;

// adapted copy of TalentGrid (database/data/talent_tree/parser/TalentGrid.java), scoped to
// AtlasNodeLayout/AtlasNode instead of TalentTree/Perk - see AtlasGridPoint for the one
// deliberate difference (node classification by registry membership, not id length)
public class AtlasGrid {

    AtlasNodeLayout layout;

    List<List<AtlasGridPoint>> grid = new ArrayList<>();

    public AtlasGridPoint get(int x, int y) {
        return grid.get(x).get(y);
    }

    public AtlasGrid(AtlasNodeLayout layout, String str) {
        this.layout = layout;

        int y = 0;
        for (String line : str.split("\n")) {
            int x = 0;
            for (String s : line.split(",")) {

                if (grid.size() <= x) {
                    grid.add(new ArrayList<>());
                }

                grid.get(x).add(new AtlasGridPoint(x, y, s));

                x++;

            }

            y++;
        }
    }

    public void loadIntoTree() {

        List<AtlasGridPoint> nodes = new ArrayList<>();

        for (List<AtlasGridPoint> list : grid) {
            for (AtlasGridPoint point : list) {
                if (point.isNode) {
                    layout.calcData.addNode(point.getPoint(), point.getId());
                    nodes.add(point);
                } else if (point.isCenter) {
                    layout.calcData.center = point.getPoint();
                }
            }
        }

        Objects.requireNonNull(layout.calcData.center, "Atlas layout needs a [CENTER]!");

        nodes.forEach(one -> {
            Set<String> connectorTypes = getConnectorTypes(one);
            nodes.forEach(two -> {
                if (!layout.calcData.isConnected(one.getPoint(), two.getPoint())) {
                    if (one.isInDistanceOf(two)) {
                        if (hasPath(one, two, connectorTypes)) {
                            layout.calcData.addConnection(one.getPoint(), two.getPoint());
                        }
                    }
                }
            });
        });
    }

    // this is very resource intensive
    private boolean hasPath(AtlasGridPoint start, AtlasGridPoint end, Set<String> connectorTypes) {

        for (String connector : connectorTypes) {

            Queue<AtlasGridPoint> openSet = new ArrayDeque<>();
            openSet.add(start);

            Set<AtlasGridPoint> closedSet = new HashSet<>();

            while (!openSet.isEmpty()) {
                AtlasGridPoint current = openSet.poll();

                if (!closedSet.add(current)) {
                    continue; // we already visited it
                }
                if (current.equals(end)) {
                    return true;
                }
                if (current.isNode && current != start) {
                    continue; // skip exploring this path
                }

                openSet.addAll(getEligibleSurroundingPoints(current, connector));
            }

        }

        return false;
    }

    Set<String> getConnectorTypes(AtlasGridPoint p) {
        Set<String> set = new HashSet<>();

        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                AtlasGridPoint c = get(p.x + x, p.y + y);
                if (c.isConnector) {
                    set.add(c.getId());
                }
            }
        }
        return set;
    }

    public Set<AtlasGridPoint> getEligibleSurroundingPoints(AtlasGridPoint p, String connector) {

        Set<AtlasGridPoint> set = new HashSet<>();
        int x = p.x;
        int y = p.y;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                AtlasGridPoint point = get(x + dx, y + dy);

                if (Math.abs(dx) == 1 && Math.abs(dy) == 1) { // we are discovering a diagonal
                    if (get(x + dx, y).isNode || get(x, y + dy).isNode) {
                        continue; // skip this diagonal, it crosses a node
                    }
                }

                if (point.isNode) {
                    set.add(point);
                } else if (point.isConnector) {
                    if (point.getId()
                            .equals(connector)) {
                        set.add(point);
                    }
                }

            }
        }

        return set;
    }

}
