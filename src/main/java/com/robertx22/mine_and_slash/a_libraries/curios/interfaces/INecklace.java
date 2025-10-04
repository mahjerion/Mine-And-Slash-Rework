package com.robertx22.mine_and_slash.a_libraries.curios.interfaces;

import com.robertx22.mine_and_slash.a_libraries.curios.CuriosSlots;

public interface INecklace extends ICuriosType {

    @Override
    public default String curioTypeName() {
        return CuriosSlots.NECKLACE.name;
    }

}
