package com.robertx22.mine_and_slash.database.data.stats.types.generated;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatScaling;
import com.robertx22.mine_and_slash.database.data.stats.effects.base.BaseDamageEffect;
import com.robertx22.mine_and_slash.database.data.stats.layers.StatLayers;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.database.data.stats.types.ElementalStat;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.AttackType;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import com.robertx22.mine_and_slash.uncommon.wrappers.MapWrapper;

import java.util.List;

public class PhysicalTakenAsElement extends ElementalStat {

    public static MapWrapper<Elements, PhysicalTakenAsElement> MAP = new MapWrapper();

    @Override
    public List<Stat> generateAllPossibleStatVariations() {
        List<Stat> list = super.generateAllPossibleStatVariations();
        list.forEach(x -> MAP.put(x.getElement(), (PhysicalTakenAsElement) x));
        return list;

    }


    public PhysicalTakenAsElement(Elements element) {
        super(element);
        this.scaling = StatScaling.NONE;

        this.statEffect = new Effect();

        this.min = 0;
        this.max = 100;
    }

    @Override
    public Stat newGeneratedInstance(Elements element) {
        return new PhysicalTakenAsElement(element);
    }

    @Override
    public String GUID() {
        return "phys_taken_as_" + this.getElement().guidName;
    }

    @Override
    public String locDescForLangFile() {
        return "Converts Physical Damage taken into Elemental Damage.";
    }

    @Override
    public String locDescLangFileGUID() {
        return SlashRef.MODID + ".stat_desc." + "turn_phys_taken_as_ele";
    }

    @Override
    public boolean IsPercent() {
        return true;
    }

    private class Effect extends BaseDamageEffect {

        @Override
        public StatPriority GetPriority() {
            return StatPriority.Damage.DAMAGE_LAYERS;
        }

        @Override
        public EffectSides Side() {
            return EffectSides.Target;
        }

        @Override
        public DamageEvent activate(DamageEvent effect, StatData data, Stat stat) {

            effect.getLayer(StatLayers.Offensive.DAMAGE_CONVERSION, EventData.NUMBER, Side()).convertDamage(getElement(), (int) data.getValue());

/*
            var conv = data.getValue();

            if (conv > effect.unconvertedDamagePercent) {
                conv = effect.unconvertedDamagePercent;
            }
            if (conv <= 0) {
                return effect;
            }
            effect.unconvertedDamagePercent -= conv;

            float dmg = effect.data.getNumber(EventData.NUMBER).number * conv / 100F;
            dmg = MathHelper.clamp(dmg, 0, effect.data.getNumber());
            if (dmg > 0) {
                effect.addBonusEleDmg(stat.getElement(), dmg, Side());
                effect.getLayer(StatLayers.Offensive.DAMAGE_CONVERSION, EventData.NUMBER, Side()).reduce(dmg);
                // or maybe create a dmg conversion layer..?
            }

 */
            return effect;
        }

        @Override
        public boolean canActivate(DamageEvent effect, StatData data, Stat stat) {
            return effect.GetElement() == Elements.Physical && effect.getAttackType().equals(AttackType.hit);
        }

    }

    @Override
    public String locNameForLangFile() {
        return "of Physical Damage Hits Taken as " + this.getElement().dmgName + " Damage";
    }

}


