package com.robertx22.mine_and_slash.database.data.stats.types.generated;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatScaling;
import com.robertx22.mine_and_slash.database.data.stats.effects.base.BaseDamageEffect;
import com.robertx22.mine_and_slash.database.data.stats.layers.StatLayers;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.AttackType;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PhysicalDamageTakenAsRandom extends Stat {

    public PhysicalDamageTakenAsRandom() {
        this.scaling = StatScaling.NONE;
        this.statEffect = new Effect();
        this.min = 0;
    }

    public static PhysicalDamageTakenAsRandom getInstance() {
        return PhysicalDamageTakenAsRandom.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final PhysicalDamageTakenAsRandom INSTANCE = new PhysicalDamageTakenAsRandom();
    }

    private static final List<Elements> ELEMENTS = Arrays.asList(
            Elements.Fire, Elements.Cold, Elements.Nature, Elements.Shadow
    );

    private static final Random RANDOM = new Random();

    @Override
    public String GUID() {
        return "phys_taken_as_random";
    }

    @Override
    public String locDescForLangFile() {
        return "Percent of physical damage will instead be taken as a random element.";
    }

    @Override
    public String locDescLangFileGUID() {
        return SlashRef.MODID + ".stat_desc." + "phys_taken_as_random";
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
            Elements randomElement = ELEMENTS.get(RANDOM.nextInt(ELEMENTS.size()));
            effect.getLayer(StatLayers.Defensive.DAMAGE_TAKEN_AS, EventData.NUMBER, Side())
                    .damageTakenAs(randomElement, (int) data.getValue());
            return effect;
        }

        @Override
        public boolean canActivate(DamageEvent effect, StatData data, Stat stat) {
            return effect.GetElement() == Elements.Physical && effect.getAttackType().equals(AttackType.hit);
        }
    }

    @Override
    public Elements getElement() {
        return Elements.Physical;
    }

    @Override
    public String locNameForLangFile() {
        return "of Physical Damage Taken as Random Elemental Damage";
    }
}