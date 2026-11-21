package com.robertx22.mine_and_slash.database.data.stats.datapacks.test;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.mine_and_slash.aoe_data.database.stats.DefenseStats;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition.RandomRollCondition;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.action.StatEffect;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition.StatCondition;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import com.robertx22.mine_and_slash.uncommon.interfaces.IStatEffect;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public class DataPackStatEffect implements IStatEffect {

    public String order = "";

    public EffectSides side = EffectSides.Source;

    public List<String> ifs = new ArrayList<>();

    public List<String> effects = new ArrayList<>();

    public List<String> events = new ArrayList<>();

    public boolean worksOnEvent(EffectEvent ev) {
        return events.contains(ev.GUID());
    }

    @Override
    public EffectSides Side() {
        return side;
    }


    public List<MutableComponent> getTooltip() {
        List<MutableComponent> t = new ArrayList<>();


        var prio = GetPriority();

        t.add(Component.literal("Priority: ").append(prio.locName()).append(" (" + +prio.priority + ")"));

        t.add(side.word.locName());

        t.add(Words.WorksOnEvent.locName());
        for (String event : events) {
            t.add(Component.literal(event));
        }

        t.add(Words.Conditions.locName());
        for (String i : ifs) {
            t.add(Component.literal("- " + i)); // todo localize all
        }
        t.add(Words.Effects.locName());
        for (String i : effects) {
            t.add(Component.literal("- " + i));// todo localize all
        }

        return t;
    }

    @Override
    public StatPriority GetPriority() {
        var p = StatPriority.MAP.get(order);
        if (p == null) {
            // todo find better error way
            ExileLog.get().warn("No such stat priority: " + order);
            return StatPriority.Spell.FIRST;
        }
        return p;
    }

    @Override
    public void TryModifyEffect(EffectEvent effect, EffectSides statSource, StatData data, Stat stat) {

        if (MMORPG.RUN_DEV_TOOLS) {
            if (stat.GUID().equals(DefenseStats.NO_SELF_DAMAGE_STATS.getId())) {
                boolean bo = true;
            }
        }

        // every condition except the random rolls first. the rolls are held back so the
        // once-per-attack check below can run between "this effect applies to this event" and
        // "roll for it"
        List<StatCondition> rolls = new ArrayList<>();
        for (String x : ifs) {
            StatCondition cond = ExileDB.StatConditions().get(x);
            if (cond == null) {
                return;
            }
            if (cond instanceof RandomRollCondition) {
                rolls.add(cond);
                continue;
            }
            if (cond.can(effect, statSource, data, stat) != cond.getConditionBoolean()) {
                return;
            }
        }

        // one attack becomes one damage event per element (added flat damage and conversion each
        // get their own bonus element event), and every one of them sweeps the stats again. without
        // this, a proc / effect application took one roll per element: an attack with added fire
        // and cold damage rolled a 30% proc three times. the first event of the attack that passes
        // the conditions takes the roll, the rest of the attack's events skip it.
        boolean alreadyRolled = false;
        if (effect instanceof DamageEvent dmg && hasOncePerAttackEffect()) {
            String key = stat.GUID() + "|" + statSource.name() + "|" + order + "|" + String.join(",", ifs) + "|" + String.join(",", effects);
            alreadyRolled = !dmg.oncePerAttackRolls.add(key);
        }

        for (StatCondition cond : rolls) {
            if (cond.can(effect, statSource, data, stat) != cond.getConditionBoolean()) {
                return;
            }
        }

        for (String x : effects) {
            StatEffect e = ExileDB.StatEffects().get(x);
            if (e == null) {
                continue;
            }
            if (alreadyRolled && e.oncePerAttack()) {
                continue;
            }
            e.activate(effect, statSource, data, stat);
        }
    }

    private boolean hasOncePerAttackEffect() {
        for (String x : effects) {
            StatEffect e = ExileDB.StatEffects().get(x);
            if (e != null && e.oncePerAttack()) {
                return true;
            }
        }
        return false;
    }
}
