package com.robertx22.mine_and_slash.database.data.stats.types.ailment;

import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.mine_and_slash.aoe_data.database.ailments.Ailment;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatGuiGroup;
import com.robertx22.mine_and_slash.database.data.stats.effects.base.BaseDamageEffect;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EventBuilder;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.AttackType;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.enumclasses.PlayStyle;
import com.robertx22.mine_and_slash.uncommon.enumclasses.WeaponTypes;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import net.minecraft.world.entity.LivingEntity;

public class AilmentChance extends Stat {

    Ailment ailment;

    public AilmentChance(Ailment ailment) {
        this.ailment = ailment;
        this.is_perc = true;
        this.statEffect = new Effect();
        this.min = 0;
        this.max = 100;

        this.gui_group = StatGuiGroup.AILMENT_CHANCE;
    }

    public static void activate(float dmg, Ailment ailment, LivingEntity source, LivingEntity target, Spell spell) {
        // we take the original or base damage of the attack so we don't double dip

        if (dmg <= 0) {
            return;
        }

        // todo will probably have to tweak this
        var event = EventBuilder.ofDamage(source, target, dmg).setupDamage(AttackType.dot, WeaponTypes.none, PlayStyle.INT).set(x -> {
            x.disableActivation = true; // we dont actually want to do the dmg now
            x.setElement(ailment.element);
            x.setisAilmentDamage(ailment);
            if (spell != null) {
                x.data.setString(EventData.SPELL, spell.GUID());
            }
        }).build();

        event.Activate();

        Load.Unit(target).ailments.onAilmentCausingDamage(source, target, ailment, event.data.getNumber());
    }

    private class Effect extends BaseDamageEffect {

        @Override
        public StatPriority GetPriority() {
            return StatPriority.Damage.FINAL_DAMAGE;
        }

        @Override
        public EffectSides Side() {
            return EffectSides.Source;
        }

        @Override
        public DamageEvent activate(DamageEvent effect, StatData data, Stat stat) {
            float dmg = effect.data.getOriginalNumber(EventData.NUMBER).number;

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
            return !effect.data.getBoolean(EventData.IS_BLOCKED) && effect.getElement() != null && effect.getElement() == ailment.element && (effect.getAttackType().isHit() || effect.getAttackType() == AttackType.bonus_dmg) && RandomUtils.roll(data.getValue());
        }

    }

    @Override
    public Elements getElement() {
        return ailment.element;
    }

    @Override
    public String locDescForLangFile() {
        return "Chance to Cause the Ailment on " + getElement().dmgName + "  hits. Maximum chance is 100%. " + ailment.locDescForLangFile();
    }

    @Override
    public String locNameForLangFile() {
        return ailment.locNameForLangFile() + " Chance";
    }

    @Override
    public String GUID() {
        return ailment.GUID() + "_chance";
    }
}
