package com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;

public class SourceAndTargetAreSame extends StatCondition {


    public SourceAndTargetAreSame() {
        super("source_is_target", "source_is_target");

    }

    @Override
    public boolean can(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {
        return event.source == event.target;
    }

    @Override
    public Class<? extends StatCondition> getSerClass() {
        return SourceAndTargetAreSame.class;
    }

}

