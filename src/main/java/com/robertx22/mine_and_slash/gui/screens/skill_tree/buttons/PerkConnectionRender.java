package com.robertx22.mine_and_slash.gui.screens.skill_tree.buttons;

import com.google.common.base.Objects;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.saveclasses.PointData;

public record PerkConnectionRender(PerkPointPair pair, Perk.Connection connection) {

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof PerkConnectionRender that)) return false;
        return Objects.equal(pair, that.pair) && connection == that.connection;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pair);
    }
}
