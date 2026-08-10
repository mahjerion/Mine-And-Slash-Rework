package com.robertx22.mine_and_slash.database.data.stats.effects.defense;

import com.robertx22.mine_and_slash.database.data.stats.IUsableStat;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.effects.base.InCodeStatEffect;
import com.robertx22.mine_and_slash.database.data.stats.layers.StatLayers;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import net.minecraft.util.Mth;

public class ArmorEffect extends InCodeStatEffect<DamageEvent> {

    public ArmorEffect() {

        super(DamageEvent.class);
    }

    @Override
    public StatPriority GetPriority() {
        return StatPriority.Damage.DAMAGE_LAYERS;
    }

    @Override
    public EffectSides Side() {
        return EffectSides.Target;
    }

    // must run even at 0 armor, otherwise leftover armor pen is silently ignored and having
    // no armor at all ends up better than having a little
    @Override
    public boolean runsOnZeroStat() {
        return true;
    }

    @Override
    public DamageEvent activate(DamageEvent effect, StatData data, Stat stat) {

        IUsableStat armor = (IUsableStat) stat;

        float afterPene = data.getValue() - effect.getPenetration();

        int points = Math.round(Math.abs(afterPene));

        if (points == 0) {
            return effect;
        }

        float EffectiveArmor = armor.getUsableValue(effect.targetData.getUnit(), points, effect.sourceData.getLevel());
        EffectiveArmor = Mth.clamp(EffectiveArmor, 0, armor.getMaxMulti());

        // so it can go in negative too if player has high armor pen
        float defense = EffectiveArmor * (afterPene > 0 ? 100F : -100F);

        effect.getLayer(StatLayers.Defensive.ARMOR_MITIGATION, EventData.NUMBER, Side()).reduce(defense);

        return effect;
    }

    @Override
    public boolean canActivate(DamageEvent effect, StatData data, Stat stat) {
        return effect.GetElement() == Elements.Physical;
    }

}
