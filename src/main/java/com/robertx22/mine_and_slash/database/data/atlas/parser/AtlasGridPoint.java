package com.robertx22.mine_and_slash.database.data.atlas.parser;

import com.google.common.hash.HashCode;
import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.mine_and_slash.saveclasses.PointData;

import java.util.Locale;

public class AtlasGridPoint {

    static int MAX_DISTANCE = 12;

    public final int x;
    public final int y;
    private final String id;

    boolean isNode = false;
    boolean isConnector = false;
    boolean isCenter = false;

    private final PointData point;

    public String getId() {
        return id;
    }

    public AtlasGridPoint(int x, int y, String str) {
        this.x = x;
        this.y = y;
        this.id = str.toLowerCase(Locale.ROOT);

        this.point = new PointData(x, y);

        // checked before the length==1/[CENTER] checks below - unlike Perk ids, AtlasNode ids
        // can be as short as 2 characters (e.g. "it"), so a length heuristic isn't reliable here
        if (DungeonDatabase.AtlasNodes().isRegistered(id)) {
            this.isNode = true;
        } else if (id.length() == 1) {
            this.isConnector = true;
        } else if (id.equalsIgnoreCase(CENTER_ID)) {
            this.isCenter = true;
        }
    }

    public boolean isInDistanceOf(AtlasGridPoint other) {
        return Math.abs(x - other.x) < MAX_DISTANCE && Math.abs(y - other.y) < MAX_DISTANCE;
    }

    public PointData getPoint() {
        return this.point;
    }

    @Override
    public int hashCode() {
        return HashCode.fromInt(x)
                .hashCode() + HashCode.fromInt(y)
                .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AtlasGridPoint) {
            AtlasGridPoint other = (AtlasGridPoint) obj;
            return other.x == x && other.y == y;
        }
        return false;
    }

    public static String CENTER_ID = "[CENTER]";

}
