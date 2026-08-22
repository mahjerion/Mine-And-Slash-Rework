package com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.DualWieldUtils;

public class IsDualWieldingCondition extends StatCondition {

    public IsDualWieldingCondition() {
        super("is_dual_wielding", "is_dual_wielding");
    }

    @Override
    public boolean can(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {
        return DualWieldUtils.isDualWielding(event.getSide(statSource));
    }

    @Override
    public Class<? extends StatCondition> getSerClass() {
        return IsDualWieldingCondition.class;
    }

}
