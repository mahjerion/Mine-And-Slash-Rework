package com.robertx22.mine_and_slash.uncommon.interfaces;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;

public interface IStatEffect {


    public boolean worksOnEvent(EffectEvent ev);

    public abstract EffectSides Side();

    public abstract StatPriority GetPriority();

    /**
     * effects that must run even when the entity's stat value is 0 (e.g. armor, so leftover
     * armor penetration still applies to an unarmored target)
     */
    public default boolean runsOnZeroStat() {
        return false;
    }

    public abstract void TryModifyEffect(EffectEvent effect, EffectSides statSource, StatData data, Stat stat);

}
