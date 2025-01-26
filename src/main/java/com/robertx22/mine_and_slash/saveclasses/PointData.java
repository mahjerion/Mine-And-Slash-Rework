package com.robertx22.mine_and_slash.saveclasses;

import java.util.Objects;

public class PointData {
    public int x;
    public int y;

    protected PointData() {

    }

    public PointData(int x, int y) {
        this.x = x;
        this.y = y;
    }


    @Override
    public String toString() {
        return x + "_" + y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointData pointData = (PointData) o;
        return x == pointData.x && y == pointData.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public PointData up() {
        return new PointData(x, y + 1);
    }

    public PointData down() {
        return new PointData(x, y - 1);
    }

    public PointData left() {
        return new PointData(x - 1, y);
    }

    public PointData right() {
        return new PointData(x + 1, y);
    }


}
