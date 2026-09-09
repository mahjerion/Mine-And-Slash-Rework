package com.robertx22.mine_and_slash.database.data.spells.components.conditions;

import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;

import java.util.Arrays;

/**
 * Passes on the first repeat of a multicast ({@code times_to_cast > 1}), and always for a single
 * cast. Before this a datapack had to fake "once per cast" with set_on_cd + is_not_on_cd on a fixed
 * tick count, which only holds at one attack speed: a fast enough recast started while the old
 * cooldown still ran and the gated component silently never fired.
 */
public class IsFirstCastCondition extends EffectCondition {

    public IsFirstCastCondition() {
        super(Arrays.asList());
    }

    @Override
    public boolean canActivate(SpellCtx ctx, MapHolder data) {
        return ctx.castNumber <= 1;
    }

    public MapHolder create() {
        MapHolder d = new MapHolder();
        d.type = GUID();
        return d;
    }

    @Override
    public String GUID() {
        return "is_first_cast";
    }
}
