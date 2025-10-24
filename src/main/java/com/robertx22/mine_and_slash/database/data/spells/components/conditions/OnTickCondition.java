package com.robertx22.mine_and_slash.database.data.spells.components.conditions;

import com.robertx22.mine_and_slash.database.data.spells.components.EntityActivation;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;

import java.util.Arrays;

import static com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField.TICK_RATE;

public class OnTickCondition extends EffectCondition {

    public OnTickCondition() {
        super(Arrays.asList(MapField.TICK_RATE));
    }

    @Override
    public boolean canActivate(SpellCtx ctx, MapHolder data) {
        if (ctx.activation != EntityActivation.ON_TICK) {
            //return false; TODO
        }
        int ticks = data.get(MapField.TICK_RATE)
            .intValue();
        int firstTick = data.getOrDefault(MapField.FIRST_TICK, 0.0)
            .intValue();

        int tickCount = ctx.sourceEntity == null ? ctx.caster.tickCount : ctx.sourceEntity.tickCount;

        if (ticks > 0) {
            return tickCount >= firstTick && tickCount % ticks == firstTick % ticks;
        } else {
            return tickCount == firstTick;
        }
    }

    public MapHolder create(Double ticks) {
        MapHolder d = new MapHolder();
        d.type = GUID();
        d.put(TICK_RATE, ticks);
        return d;
    }

    @Override
    public String GUID() {
        return "x_ticks_condition";
    }
}
