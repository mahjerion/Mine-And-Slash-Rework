package com.robertx22.mine_and_slash.uncommon.effectdatas.rework.action;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageInitEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;

public class DisableSourceStatsEffect extends StatEffect {

    public DisableSourceStatsEffect() {
        super("disable_attacker_stats", "disable_attacker_stats");
    }

    @Override
    public void activate(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {
        if (event instanceof DamageInitEvent e) {
            e.dmg.calcSourceEffects = false;
        }
    }

    @Override
    public Class<? extends StatEffect> getSerClass() {
        return DisableSourceStatsEffect.class;
    }
}