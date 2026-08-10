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

public class AilmentReceiveChance extends Stat {

    Ailment ailment;

    public AilmentReceiveChance(Ailment ailment) {
        this.minus_is_good = true;
        this.ailment = ailment;
        this.is_perc = true;
        this.statEffect = new Effect();
        this.min = 0;
        this.max = 100;

        this.gui_group = StatGuiGroup.AILMENT_CHANCE;
    }


    private class Effect extends BaseDamageEffect {

        @Override
        public StatPriority GetPriority() {
            // has to match AilmentChance: the flat damage layer isn't populated yet at Spell.FIRST
            return StatPriority.Damage.FINAL_DAMAGE;
        }

        @Override
        public EffectSides Side() {
            return EffectSides.Target;
        }

        @Override
        public DamageEvent activate(DamageEvent effect, StatData data, Stat stat) {
            // same base as AilmentChance - see the comment there on why the flat damage layer is added
            float dmg = effect.data.getOriginalNumber(EventData.NUMBER).number + effect.getAppliedFlatDamage();

            //if the dmg was converted, lower the base ailment damage
            float convMulti = effect.unconvertedDamagePercent / 100F;
            dmg *= convMulti;

            AilmentChance.activate(dmg, ailment, effect.source, effect.target, effect.getSpellOrNull());
            return effect;
        }

        @Override
        public boolean canActivate(DamageEvent effect, StatData data, Stat stat) {
            if (effect.data.getNumber() <= 0) {
                return false;
            }
            if (effect.unconvertedDamagePercent <= 0) {
                return false;
            }
            if (effect.data.getBoolean(EventData.IS_DODGED)) {
                return false;
            }
            return !effect.data.getBoolean(EventData.IS_BLOCKED) && effect.getElement() != null && effect.getElement() == ailment.element && (effect.getAttackType().isHit() || effect.getAttackType() == AttackType.bonus_dmg) && RandomUtils.roll(data.getValue());
        }

    }

    @Override
    public Elements getElement() {
        return ailment.element;
    }

    @Override
    public String locDescForLangFile() {
        return "Chance to Receive the Ailment on " + getElement().dmgName + "  hits. Maximum chance is 100%. " + ailment.locDescForLangFile();
    }

    @Override
    public String locNameForLangFile() {
        return "Chance to Receive " + ailment.locNameForLangFile();
    }

    @Override
    public String GUID() {
        return ailment.GUID() + "_receive_chance";
    }
}
