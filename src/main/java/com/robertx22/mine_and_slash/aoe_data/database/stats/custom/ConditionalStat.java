package com.robertx22.mine_and_slash.aoe_data.database.stats.custom;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition.StatCondition;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import com.robertx22.mine_and_slash.uncommon.interfaces.IStatEffect;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;


public class ConditionalStat extends Stat {

    private final Stat baseStat;
    private final StatCondition cond;
    private final String id;
    private final String locName;

    public ConditionalStat(String id, String locName, Stat baseStat, StatCondition cond) {
        this.baseStat = baseStat;
        this.cond = cond;
        this.id = id;
        this.locName = locName;

        // ---- copy public fields for UI/tooltip correctness ----
        this.min = baseStat.min;
        this.max = baseStat.max;
        this.base = baseStat.base;
        this.is_perc = baseStat.is_perc;
        this.scaling = baseStat.scaling;
        this.is_long = baseStat.is_long;
        this.icon = baseStat.icon;
        this.order = baseStat.order;
        this.format = baseStat.format;
        this.group = baseStat.group;
        this.minus_is_good = baseStat.minus_is_good;
        this.show_in_gui = baseStat.show_in_gui;
        this.gui_group = baseStat.gui_group;

        // context modifier (if any) passes through unchanged
        this.statContextModifier = baseStat.statContextModifier;

        // Wrap the effect with a conditional guard if one exists
        if (baseStat.statEffect != null) {
            this.statEffect = new ConditionalEffect(baseStat.statEffect, cond, this);
        } else {
            this.statEffect = null;
        }
    }

    // ---------------- IGUID / localization / registry ----------------

    @Override
    public String GUID() {
        return baseStat.GUID();
    }

    @Override
    public String locNameForLangFile() {
        return (locName != null && !locName.isEmpty()) ? locName : baseStat.locNameForLangFile();
    }

    @Override
    public String locDescForLangFile() {
        return baseStat.locDescForLangFile();
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.STAT;
    }

    // ---------------- Stat identity ----------------

    @Override
    public Elements getElement() {
        return baseStat.getElement();
    }

    // ---------------- Conditional effect ----------------

    private static class ConditionalEffect implements IStatEffect {

        private final IStatEffect inner;
        private final StatCondition cond;
        private final Stat owner;

        ConditionalEffect(IStatEffect inner, StatCondition cond, Stat owner) {
            this.inner = inner;
            this.cond = cond;
            this.owner = owner;
        }

        @Override
        public EffectSides Side() {
            return inner.Side();
        }

        // NOTE: In this branch GetPriority returns StatPriority, not int.
        @Override
        public StatPriority GetPriority() {
            return inner.GetPriority();
        }

        @Override
        public boolean worksOnEvent(EffectEvent event) {
            return inner.worksOnEvent(event);
        }

        @Override
        public void TryModifyEffect(EffectEvent event, EffectSides side, StatData data, Stat stat) {
        // Gate by our condition; do nothing if it fails
            if (cond != null && !cond.can(event, side, data, owner)) {
                return;
            }
            inner.TryModifyEffect(event, side, data, owner);
        }
    }
}
