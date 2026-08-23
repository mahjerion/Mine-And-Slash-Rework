package com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;

public class IsNotOnCooldownCondition extends StatCondition {

    String cd_id = "";

    public IsNotOnCooldownCondition(String cdid) {
        this("is_" + cdid + "_not_on_cd", cdid);
    }

    // id and key are separate so the key can move without renaming the entry. the proc conditions
    // keep their historical ids while pointing at ProcSpellEffect.procCooldownKey, which is what
    // keeps every stat that references them by id from needing to change
    public IsNotOnCooldownCondition(String id, String cdid) {
        super(id, "is_not_on_cd");
        this.cd_id = cdid;
    }

    IsNotOnCooldownCondition() {
        super("", "is_not_on_cd");
    }

    @Override
    public boolean can(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {
        return !Load.Unit(event.getSide(statSource)).getCooldowns().isOnCooldown(cd_id);
    }

    @Override
    public Class<? extends StatCondition> getSerClass() {
        return IsNotOnCooldownCondition.class;
    }

}
