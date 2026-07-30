package com.robertx22.mine_and_slash.aoe_data.database.spells;

import com.robertx22.library_of_exile.registry.IGUID;

import java.util.ArrayList;
import java.util.List;

public enum SummonType implements IGUID {
    GOLEM("golem", "Golem", 1),
    UNDEAD("undead", "Undead", 3),
    SPIDER("spider", "Spider", 3),
    BEAST("beast", "Beast", 1),
    NONE("none", "NONE", 0);

    public String id;
    public String name;
    public int maxSummons;

    SummonType(String id, String name, int maxSummons) {
        this.id = id;
        this.name = name;
        this.maxSummons = maxSummons;
    }

    @Override
    public String GUID() {
        return id;
    }

    // NONE means uncapped, it must never get a max summons stat generated for it
    public static List<SummonType> getCapped() {
        List<SummonType> list = new ArrayList<>();
        for (SummonType x : values()) {
            if (x != NONE) {
                list.add(x);
            }
        }
        return list;
    }

    public static SummonType fromId(String id) {
        for (SummonType x : values()) {
            if (x.id.equals(id)) {
                return x;
            }
        }
        return NONE;
    }
}
