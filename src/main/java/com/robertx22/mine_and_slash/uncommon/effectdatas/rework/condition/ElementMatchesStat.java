package com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;

public class ElementMatchesStat extends StatCondition {

    public ElementMatchesStat() {
        super("ele_match_stat", "ele_match_stat");
    }

    @Override
    public boolean can(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {
        if (event instanceof DamageEvent dmg) {
            // a fully converted (or fully "taken as") hit keeps its original element label but
            // carries none of that element's damage any more - it was all moved into a separate
            // bonus element event. the label alone would still match, so a 100% phys-to-fire crit
            // kept proccing "on physical crit" stats like hemorrhage. mirrors the guard in
            // AilmentChance.canActivate. both counters are per event, so the bonus element event
            // built from the converted damage (a fresh event at 100/100) still matches its own element.
            // stats at damage_layers or earlier run before conversion and never see these below 100.
            if (dmg.unconvertedDamagePercent <= 0 || dmg.unconvertedDamageTakenAsPercent <= 0) {
                return false;
            }
        }
        return event.data.getElement().elementsMatch(stat.getElement());
    }

    @Override
    public Class<? extends StatCondition> getSerClass() {
        return ElementMatchesStat.class;
    }

}
