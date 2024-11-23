package com.robertx22.mine_and_slash.aoe_data.database.stats;

import com.robertx22.mine_and_slash.aoe_data.database.spells.schools.ProcSpells;
import com.robertx22.mine_and_slash.aoe_data.database.stat_conditions.StatConditions;
import com.robertx22.mine_and_slash.aoe_data.database.stat_effects.StatEffects;
import com.robertx22.mine_and_slash.aoe_data.database.stats.base.DatapackStatBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.stats.base.EmptyAccessor;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.datapacks.test.DataPackStatAccessor;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.uncommon.effectdatas.OnDeathEvent;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;

import static com.robertx22.mine_and_slash.database.data.stats.Stat.VAL1;

public class MobDeathStats {

    public static DataPackStatAccessor<EmptyAccessor> IGNITE_EXPLODE_ON_KILL = DatapackStatBuilder
            .ofSingle("on_death_ignite_explosion", Elements.Fire)
            .worksWithEvent(OnDeathEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.IF_RANDOM_ROLL)
            .addCondition(StatConditions.IS_NOT_ON_COOLDOWN.get(ProcSpells.IGNITE_EXPLOSION))
            .addEffect(e -> StatEffects.PROC_IGNITE_EXPLOSION)
            .setLocName(x -> Stat.format(VAL1 + "% Chance to cause an Ignited Explosion on Death."))
            .setLocDesc(x -> "This has a short cooldown. You can see the whole spell stats in the Ingame Library")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.is_long = true;
                x.max = 100;
            })
            .build();

    public static void init() {

    }

}
