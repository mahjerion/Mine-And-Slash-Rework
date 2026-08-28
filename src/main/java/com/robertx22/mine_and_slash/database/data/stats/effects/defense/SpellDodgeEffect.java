package com.robertx22.mine_and_slash.database.data.stats.effects.defense;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.effects.base.BaseDamageEffect;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.SpellDodge;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.AttackType;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import com.robertx22.mine_and_slash.capability.entity.AvoidanceEntropyData;
import net.minecraft.util.Mth;

public class SpellDodgeEffect extends BaseDamageEffect {

    protected SpellDodgeEffect() {
    }

    public static SpellDodgeEffect getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public StatPriority GetPriority() {
        return StatPriority.Damage.HIT_PREVENTION;
    }

    @Override
    public EffectSides Side() {
        return EffectSides.Target;
    }

    @Override
    public DamageEvent activate(DamageEvent effect, StatData data, Stat stat) {
        SpellDodge dodge = (SpellDodge) stat;

        float totalDodge = Mth.clamp(data.getValue() - effect.data.getNumber(EventData.ACCURACY).number, 0, Integer.MAX_VALUE);

        float chance = dodge.getUsableValue(effect.targetData.getUnit(), (int) totalDodge, effect.sourceData.getLevel()) * 100;

        // means "this attack already got its one avoidance answer", not "it was dodged" - the element
        // splits inherit it either way
        effect.data.setBoolean(EventData.AVOIDANCE_ROLLED, true);

        // entropy instead of a per hit roll. same long run rate, but spread evenly - see
        // AvoidanceEntropyData
        if (effect.targetData.avoidanceEntropy.rollAvoid(AvoidanceEntropyData.SPELL_DODGE, effect.target.level()
                .getGameTime(), chance)) {
            effect.data.setHitAvoided(EventData.IS_DODGED);
        }

        return effect;
    }

    @Override
    public boolean canActivate(DamageEvent effect, StatData data, Stat stat) {
        if (!effect.canAvoidHit()) {
            return false;
        }
        // an outcome costs entropy now, so don't spend it on a hit something else already avoided
        if (effect.data.isHitAvoided()) {
            return false;
        }
        // every element split of a spell carries the spell id, so this effect used to roll on the parent
        // AND on each split. that was survivable on an independent roll, but each roll charges the entropy
        // counter, so one converted spell would charge it three or four times and push the realised rate
        // well past the listed one. the parent decides, the splits inherit.
        if (effect.data.getBoolean(EventData.AVOIDANCE_ROLLED)) {
            return false;
        }
        // a dot tick isn't a hit - you can't dodge a burn that already landed. DodgeRating has always
        // excluded dots, this brings spell dodge in line and stops a ticking dot from draining the pool
        // that real hits should be spending.
        if (!effect.getAttackType().isHit() && effect.getAttackType() != AttackType.bonus_dmg) {
            return false;
        }
        return effect.isSpell() && effect.getSpell().config.tags.contains(SpellTags.magic);
    }

    private static class SingletonHolder {
        private static final SpellDodgeEffect INSTANCE = new SpellDodgeEffect();
    }
}
