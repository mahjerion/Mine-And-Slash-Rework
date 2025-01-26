package com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import net.minecraft.world.entity.LivingEntity;

public class IsTargetLow extends StatCondition {

    EffectSides side;
    int perc;


    boolean check_combined_hp_and_ms = true;

    public IsTargetLow(String id, int percent, EffectSides side) {
        super(id, "is_target_low");
        this.side = side;
        this.perc = percent;
    }

    IsTargetLow() {
        super("", "is_target_low");
    }

    @Override
    public boolean can(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {

        LivingEntity en = event.getSide(side);
        var endata = Load.Unit(en);
        float ms = endata.getResources().getMagicShield();
        float msmax = endata.getUnit().magicShieldData().getValue();

        float hp = en.getHealth();
        float maxhp = en.getMaxHealth();

        if (check_combined_hp_and_ms) {
            float current = ms + hp;
            float max = msmax + maxhp;
            return perc > current / max * 100;
        }
        if (maxhp > msmax) {
            return perc > hp / maxhp * 100;
        } else {
            return perc > ms / msmax * 100;
        }


    }

    @Override
    public Class<? extends StatCondition> getSerClass() {
        return IsTargetLow.class;
    }

}
