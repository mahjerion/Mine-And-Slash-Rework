package com.robertx22.mine_and_slash.database.data.stats.types.defense;

import com.robertx22.library_of_exile.util.UNICODE;
import com.robertx22.mine_and_slash.capability.entity.AvoidanceEntropyData;
import com.robertx22.mine_and_slash.database.data.stats.IUsableStat;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatScaling;
import com.robertx22.mine_and_slash.database.data.stats.effects.base.BaseDamageEffect;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.AttackType;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;

public class DodgeRating extends Stat implements IUsableStat {

    public static String GUID = "dodge";

    public static DodgeRating getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public String locDescForLangFile() {
        return "Chance to ignore attack damage. Dodges are spread evenly across the hits you take instead of being rolled per hit.";
    }

    private DodgeRating() {
        this.min = 0;
        this.scaling = StatScaling.NORMAL;
        this.group = StatGroup.MAIN;

        this.statEffect = new Effect();

        this.icon = UNICODE.STAR;
        this.format = ChatFormatting.DARK_GREEN.getName();

    }

    @Override
    public String GUID() {
        return GUID;
    }

    @Override
    public Elements getElement() {
        return Elements.Physical;
    }

    @Override
    public boolean IsPercent() {
        return false;
    }

    @Override
    public String locNameForLangFile() {
        return "Dodge Rating";
    }

    @Override
    public float getMaxMulti() {
        return 0.8F;
    }

    @Override
    public float valueNeededToReachMaximumPercentAtLevelOne() {
        return 100;
    }

    private static class Effect extends BaseDamageEffect {

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

            DodgeRating dodge = (DodgeRating) stat;

            float totalDodge = Mth.clamp(data.getValue() - effect.data.getNumber(EventData.ACCURACY).number, 0, Integer.MAX_VALUE);

            float chance = dodge.getUsableValue(effect.targetData.getUnit(), (int) totalDodge, effect.sourceData.getLevel()) * 100;

            // mark the decision as made before it's made - the flag means "this attack already got its one
            // avoidance answer", not "it was dodged", and the element splits inherit it either way
            effect.data.setBoolean(EventData.AVOIDANCE_ROLLED, true);

            // entropy instead of a per hit roll. same long run rate, but spread evenly - see
            // AvoidanceEntropyData
            if (effect.targetData.avoidanceEntropy.rollAvoid(AvoidanceEntropyData.DODGE, effect.target.level()
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
            // a block, or the raised vanilla shield, already avoided this one. an outcome costs entropy
            // now, so don't spend it on a hit that was already going to deal nothing. mirrors the guard
            // BlockChance has for the same reason.
            if (effect.data.isHitAvoided()) {
                return false;
            }
            // the inherited case - the parent hit already resolved dodge and this is one element split of
            // it, so it takes that answer instead of rolling (and charging) a second time
            if (effect.data.getBoolean(EventData.AVOIDANCE_ROLLED)) {
                return false;
            }
            // no element gate on purpose: Dodge Rating answers for attacks the way Spell Dodge
            // answers for magic. a natively elemental non magic hit (flame_strike is fire + melee,
            // tidal_strike is cold + melee) used to fall between the two - refused here on element
            // and there on the missing magic tag - and was dodgeable by nothing at all.
            //
            // bonus_dmg is kept for safety rather than need now. with no element gate the parent
            // event is always eligible and always resolves first - calculateEffects() finishes
            // before activate() builds a single child - so every element split inherits the parent's
            // answer through AVOIDANCE_ROLLED above. that flag, not the element, is what stops one
            // attack from charging the entropy pool once per split.
            if (!effect.getAttackType().isHit() && effect.getAttackType() != AttackType.bonus_dmg) {
                return false;
            }
            if (effect.isSpell() && effect.getSpell().config.tags.contains(SpellTags.magic)) {
                return false;
            }
            return true;
        }
    }

    private static class SingletonHolder {
        private static final DodgeRating INSTANCE = new DodgeRating();
    }
}
