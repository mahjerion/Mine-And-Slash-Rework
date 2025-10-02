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

public class PhysicalToRandom extends Stat {

    public PhysicalToRandom() {
        this.scaling = StatScaling.NONE;
        this.statEffect = new Effect();
        this.min = 0;
    }

    public static PhysicalToRandom getInstance() {
        return PhysicalToRandom.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final PhysicalToRandom INSTANCE = new PhysicalToRandom();
    }

    private static final List<Elements> ELEMENTS = Arrays.asList(
            Elements.Fire, Elements.Cold, Elements.Nature, Elements.Shadow
    );
    private static final Random RANDOM = new Random();

    private class Effect extends BaseDamageEffect {

        @Override
        public StatPriority GetPriority() {
            return StatPriority.Damage.DAMAGE_LAYERS;
        }

        @Override
        public EffectSides Side() {
            return EffectSides.Source;
        }

        @Override
        public DamageEvent activate(DamageEvent effect, StatData data, Stat stat) {
            // Pick a random element (not Physical)
            Elements randomElement = ELEMENTS.get(RANDOM.nextInt(ELEMENTS.size()));
            // Convert the specified percentage of physical damage to the random element
            effect.getLayer(StatLayers.Offensive.DAMAGE_CONVERSION, EventData.NUMBER, Side())
                    .convertDamage(randomElement, (int) data.getValue());
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
    public String locDescForLangFile() {
        return "Converts a percentage of physical hit damage to a random element.";
    }

    @Override
    public String locNameForLangFile() {
        return "Physical to Random Element Damage";
    }

    @Override
    public String GUID() {
        return "phys_to_random";
    }

    @Override
    public boolean IsPercent() {
        return true;
    }

    @Override
    public String locDescLangFileGUID() {
        return SlashRef.MODID + ".stat_desc." + "turn_phys_to_random";
    }
}