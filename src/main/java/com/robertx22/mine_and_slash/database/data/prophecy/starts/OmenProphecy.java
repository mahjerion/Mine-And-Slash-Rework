package com.robertx22.mine_and_slash.database.data.prophecy.starts;

import com.robertx22.mine_and_slash.database.data.omen.OmenBlueprint;
import com.robertx22.mine_and_slash.database.data.prophecy.ProphecyModifierType;
import com.robertx22.mine_and_slash.database.data.prophecy.ProphecyStart;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.ItemBlueprint;

public class OmenProphecy extends ProphecyStart {
    @Override
    public ItemBlueprint create(int lvl, int tier) {
        var info = LootInfo.ofLevel(lvl);
        info.map_tier = tier;

        return new OmenBlueprint(info);
    }

    @Override
    public boolean acceptsModifier(ProphecyModifierType type) {
        return type == ProphecyModifierType.OMEN_RARITY;
    }

    // an omen is a curio-slot item with set bonuses, so an omen offer is worth more than a jewel one
    @Override
    public float costMulti() {
        return 1.5F;
    }

    @Override
    public String GUID() {
        return "omen";
    }

    @Override
    public int Weight() {
        return 100;
    }

    @Override
    public String locNameForLangFile() {
        return "Omen Prophecy";
    }
}
