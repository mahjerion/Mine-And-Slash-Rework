package com.robertx22.mine_and_slash.database.data.stats.types.generated;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatScaling;
import com.robertx22.mine_and_slash.database.data.stats.effects.base.BaseDamageEffect;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.database.data.stats.types.ElementalStat;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.number_provider.NumberModifier;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.enumclasses.PlayStyle;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;

import java.util.ArrayList;
import java.util.List;

public class BonusFlatAttackElementalDamage extends ElementalStat {

    @Override
    public List<Stat> generateAllPossibleStatVariations() {
        List<Stat> list = new ArrayList<>();
        Elements.getAllSingle()
                .forEach(x -> list.add(newGeneratedInstance(x)));
        return list;
    }

    public BonusFlatAttackElementalDamage(Elements element) {
        super(element);
        this.scaling = StatScaling.NORMAL;
        this.group = StatGroup.ELEMENTAL;
        this.statEffect = new Effect();

        this.format = element.format.getName();
        this.icon = element.icon;

    }

    @Override
    public Stat newGeneratedInstance(Elements element) {
        return new BonusFlatAttackElementalDamage(element);
    }

    @Override
    public boolean IsPercent() {
        return false;
    }

    @Override
    public String locDescLangFileGUID() {
        return SlashRef.MODID + ".stat_desc." + "ele_added_attack_dmg";
    }

    @Override
    public String locNameForLangFile() {
        return "Added " + getElement().dmgName + " Damage to Attacks";
    }

    @Override
    public String locDescForLangFile() {
        return "Adds x element damage on hit with an Attack. It's multiplied by damage effectiveness of the hit.";
    }

    @Override
    public String GUID() {
        return "flat_" + this.getElement().guidName + "_added_attack_damage";
    }

    private static class Effect extends BaseDamageEffect {

        @Override
        public StatPriority GetPriority() {
            return StatPriority.Damage.BEFORE_DAMAGE_LAYERS;
        }

        @Override
        public EffectSides Side() {
            return EffectSides.Source;
        }

        @Override
        public DamageEvent activate(DamageEvent effect, StatData data, Stat stat) {
            float num = NumberModifier.ModifierType.SPELL_DAMAGE_EFFECTIVENESS_MULTI.modify(effect, data.getValue());
            effect.addBonusEleDmg(stat.getElement(), num, Side());
            return effect;
        }

        @Override
        public boolean canActivate(DamageEvent effect, StatData data, Stat stat) {
            return effect.data.getAttackType().isHit()
                    && !effect.data.getBoolean(EventData.IS_BONUS_ELEMENT_DAMAGE)
                    && effect.data.getStyle() != PlayStyle.INT;
        }

    }

}
