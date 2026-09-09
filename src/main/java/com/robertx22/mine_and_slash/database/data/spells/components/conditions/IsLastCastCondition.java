package com.robertx22.mine_and_slash.database.data.spells.components.conditions;

import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;

import java.util.Arrays;

/**
 * Passes on the final repeat of a multicast ({@code times_to_cast > 1}), and always for a single
 * cast. The counterpart of {@link IsFirstCastCondition}, for finisher hits.
 */
public class IsLastCastCondition extends EffectCondition {

    public IsLastCastCondition() {
        super(Arrays.asList());
    }

    @Override
    public boolean canActivate(SpellCtx ctx, MapHolder data) {
        return ctx.castNumber >= ctx.castsTotal;
    }

    public MapHolder create() {
        MapHolder d = new MapHolder();
        d.type = GUID();
        return d;
    }

    @Override
    public String GUID() {
        return "is_last_cast";
    }
}
