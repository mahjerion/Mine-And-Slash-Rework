package com.robertx22.mine_and_slash.aoe_data.database.item_sets;

import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.item_set.ItemSet;
import com.robertx22.mine_and_slash.database.data.item_set.SetBonus;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;

import java.util.Arrays;
import java.util.List;

public class ItemSetBuilder {

    private final ItemSet set = new ItemSet();

    public static ItemSetBuilder of(String id, String name, String... uniques) {
        ItemSetBuilder b = new ItemSetBuilder();
        b.set.id = id;
        b.set.name = name;
        b.set.uniques.addAll(Arrays.asList(uniques));
        return b;
    }

    public ItemSetBuilder bonus(int pieces, List<StatMod> stats) {
        set.bonuses.add(new SetBonus(pieces, stats));
        return this;
    }

    public ItemSet build() {
        set.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        return set;
    }
}
