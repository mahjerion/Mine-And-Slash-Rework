package com.robertx22.mine_and_slash.a_libraries.curios.interfaces;

import com.robertx22.mine_and_slash.a_libraries.curios.CuriosSlots;

public interface IRing extends ICuriosType {
    @Override
    public default String curioTypeName() {
        return CuriosSlots.RING.name;
    }
}
