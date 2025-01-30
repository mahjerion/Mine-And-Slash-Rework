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

public class PhysicalDamageTakenAs extends ElementalStat {

    public static MapWrapper<Elements, PhysicalDamageTakenAs> MAP = new MapWrapper();

    @Override
    public List<Stat> generateAllPossibleStatVariations() {
        List<Stat> list = super.generateAllPossibleStatVariations();
        list.forEach(x -> MAP.put(x.getElement(), (PhysicalDamageTakenAs) x));
        return list;

    }

    public PhysicalDamageTakenAs(Elements element) {
        super(element);
        this.scaling = StatScaling.NONE;

        this.statEffect = new Effect();

        this.min = 0;
        this.max = 100;
    }

    @Override
    public Stat newGeneratedInstance(Elements element) {
        return new PhysicalDamageTakenAs(element);
    }

    @Override
    public String GUID() {
        return "phys_taken_as_" + this.getElement().guidName;
    }

    @Override
    public String locDescForLangFile() {
        return "Percent of physical damage will instead by taken as x element.";
    }

    @Override
    public String locDescLangFileGUID() {
        return SlashRef.MODID + ".stat_desc." + "phys_taken_as";
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
            effect.getLayer(StatLayers.Defensive.DAMAGE_TAKEN_AS, EventData.NUMBER, Side()).damageTakenAs(getElement(), (int) data.getValue());
            return effect;
        }

        @Override
        public boolean canActivate(DamageEvent effect, StatData data, Stat stat) {
            return effect.GetElement() == Elements.Physical && effect.getAttackType().equals(AttackType.hit);
        }
    }

    @Override
    public String locNameForLangFile() {
        return "Physical damage taken as " + this.getElement().dmgName + " Damage";
    }

}

