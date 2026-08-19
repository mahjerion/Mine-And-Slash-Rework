package com.robertx22.mine_and_slash.database.data.spells.components.conditions;

import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;

import java.util.Arrays;
import java.util.List;

public class OrCondition extends EffectCondition {

    public OrCondition() {
        super(Arrays.asList(MapField.MAPS));
    }

    @Override
    public boolean canActivate(SpellCtx ctx, MapHolder data) {
        return data.get(MapField.MAPS).stream().anyMatch(condition -> EffectCondition.conditionPasses(condition, ctx));
    }

    public MapHolder create(List<MapHolder> conditions) {
        MapHolder d = new MapHolder();
        d.type = GUID();
        d.put(MapField.MAPS, conditions);
        return d;
    }

    @Override
    public String GUID() {
        return "or";
    }
}
