package com.robertx22.mine_and_slash.gui.screens.skill_tree.buttons;

import com.robertx22.mine_and_slash.saveclasses.PointData;

public record PerkPointPair(PointData data1, PointData data2) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PerkPointPair that = (PerkPointPair) o;

        if (data1 != null ? !data1.equals(that.data1) : that.data1 != null) return false;
        return data2 != null ? data2.equals(that.data2) : that.data2 == null;
    }

    @Override
    public int hashCode() {
        int result = data1 != null ? data1.hashCode() : 0;
        result = 31 * result + (data2 != null ? data2.hashCode() : 0);
        return result;
    }
}
