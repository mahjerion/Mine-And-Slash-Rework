package com.robertx22.mine_and_slash.gui.screens.skill_tree.buttons;

import com.robertx22.mine_and_slash.saveclasses.PointData;

public record PerkPointPair(PointData data1, PointData data2) {

    @Override
    public boolean equals(Object o) {
        if (o instanceof PerkPointPair p) {
            return (data1.equals(p.data1) || data1.equals(p.data2)) && (data2.equals(p.data1) || data2.equals(p.data2));
        }
        return false;
    }
    //the order of data1 and data2 will not change the hashcode of pair.
    @Override
    public int hashCode() {
        int hashA = data1.hashCode();
        int hashB = data2.hashCode();

        long result = 17;
        result = 31 * result + (hashA ^ hashB);
        result = 31 * result + (hashA & hashB);
        result = 31 * result + (hashA | hashB);

        return (int) result;
    }

}
