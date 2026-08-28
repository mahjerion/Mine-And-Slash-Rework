package com.robertx22.mine_and_slash.database.data.stats.types.ailment;

import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.mine_and_slash.aoe_data.database.ailments.Ailment;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatGuiGroup;
import com.robertx22.mine_and_slash.database.data.stats.effects.base.BaseDamageEffect;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.AttackType;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;

public class AilmentProcStat extends Stat {

    Ailment ailment;

    public AilmentProcStat(Ailment ailment) {
        this.ailment = ailment;
        this.is_perc = true;
        // a roll can never do better than always, and AilmentChance caps itself the same way
        this.min = 0;
        this.max = 100;

        this.statEffect = new Effect();
        this.gui_group = StatGuiGroup.AILMENT_PROC_CHANCE;
    }

    private class Effect extends BaseDamageEffect {

        @Override
        public StatPriority GetPriority() {
            // strictly after AilmentChance at FINAL_DAMAGE. both used to sit on the same
            // priority, and ties keep stat map order, so whether the hit that applied the
            // ailment also fed the burst it set off was left to chance
            return StatPriority.Damage.POST_FINAL_DAMAGE_CHECKS;
        }

        @Override
        public EffectSides Side() {
            return EffectSides.Source;
        }

        @Override
        public DamageEvent activate(DamageEvent effect, StatData data, Stat stat) {
            if (effect.targetData.ailments.shatterAccumulated(effect.source, effect.target, ailment, effect.getSpellOrNull())) {
                // only flag the hit when a burst actually happened - stats and conditions read
                // this to mean damage was released
                effect.data.setBoolean(EventData.AILMENT_PROCCED, true);
            }
            return effect;
        }

        @Override
        public boolean canActivate(DamageEvent effect, StatData data, Stat stat) {
            // these guards mirror AilmentChance. without them a dodged or blocked hit still
            // detonated the whole accumulated pool
            if (effect.data.getNumber() <= 0) {
                return false;
            }
            if (effect.unconvertedDamagePercent <= 0) {
                return false;
            }
            if (effect.data.getBoolean(EventData.IS_DODGED)) {
                return false;
            }
            if (effect.data.getBoolean(EventData.IS_BLOCKED)) {
                return false;
            }
            if (effect.getElement() == null || effect.getElement() != ailment.element) {
                return false;
            }
            if (!effect.getAttackType().isHit() && effect.getAttackType() != AttackType.bonus_dmg) {
                return false;
            }
            // check the pool before rolling, so an empty one neither burns the roll nor
            // reports a proc that released nothing
            if (!effect.targetData.ailments.hasAccumulated(effect.source, ailment)) {
                return false;
            }
            return RandomUtils.roll(data.getValue());
        }

    }

    @Override
    public Elements getElement() {
        return ailment.element;
    }

    @Override
    public String locDescForLangFile() {
        return "Procs the accumulated damage of the ailment";
    }

    @Override
    public String locNameForLangFile() {
        return ailment.procNameWord().locNameForLangFile() + " Chance";
    }


    @Override
    public String GUID() {
        return ailment.GUID() + "_proc_chance";
    }
}
