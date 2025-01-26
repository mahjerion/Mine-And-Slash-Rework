package com.robertx22.mine_and_slash.gui.screens.skill_tree.buttons;

import com.robertx22.mine_and_slash.saveclasses.PointData;

import java.util.Objects;

public record PerkPointPair(PointData data1, PointData data2) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerkPointPair that = (PerkPointPair) o;
        return Objects.equals(data1, that.data1) && Objects.equals(data2, that.data2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data1, data2);
    }
}
