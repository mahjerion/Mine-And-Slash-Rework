package com.robertx22.mine_and_slash.database.data.stats.types.generated;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatScaling;
import com.robertx22.mine_and_slash.database.data.stats.effects.base.BaseDamageEffect;
import com.robertx22.mine_and_slash.database.data.stats.layers.StatLayers;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.tags.all.ElementTags;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.AttackType;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;

public class ElementalToChaos extends Stat {

    public ElementalToChaos() {
        this.scaling = StatScaling.NONE;
        this.statEffect = new Effect();
        this.min = 0;
    }

    public static ElementalToChaos getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final ElementalToChaos INSTANCE = new ElementalToChaos();
    }

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
            effect.getLayer(StatLayers.Offensive.DAMAGE_CONVERSION, EventData.NUMBER, Side())
                    .convertDamage(Elements.Shadow, (int) data.getValue());
            return effect;
        }

        @Override
        public boolean canActivate(DamageEvent effect, StatData data, Stat stat) {
            // see PhysicalToElement - bonus_dmg carries added flat elemental damage on skills of
            // another element, it never touches the main number so gating on hit alone misses it
            return effect.GetElement().tags.contains(ElementTags.ELEMENTAL)
                    && effect.GetElement().isSingleElement()
                    && (effect.getAttackType().isHit() || effect.getAttackType() == AttackType.bonus_dmg)
                    && effect.conversionDepth < DamageEvent.MAX_CONVERSION_DEPTH;
        }
    }

    @Override
    public Elements getElement() {
        return Elements.Shadow;
    }

    @Override
    public String locDescForLangFile() {
        return "Converts a percentage of fire, cold and lightning hit damage into chaos damage.";
    }

    @Override
    public String locNameForLangFile() {
        return "Elemental to Chaos Damage";
    }

    @Override
    public String GUID() {
        return "ele_to_chaos";
    }

    @Override
    public boolean IsPercent() {
        return true;
    }

    @Override
    public String locDescLangFileGUID() {
        return SlashRef.MODID + ".stat_desc." + "turn_ele_to_chaos";
    }
}
