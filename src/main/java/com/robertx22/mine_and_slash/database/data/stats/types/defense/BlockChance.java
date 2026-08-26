package com.robertx22.mine_and_slash.database.data.stats.types.defense;

import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.stats.IUsableStat;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.effects.base.BaseDamageEffect;
import com.robertx22.mine_and_slash.database.data.stats.layers.StatLayers;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.saveclasses.unit.Unit;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.AttackType;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ShieldItem;

public class BlockChance extends Stat implements IUsableStat {

    public static String GUID = "block_chance";

    // not a registered spell id on purpose, so tickSpellCooldowns skips it and a cooldown refresh
    // skill cannot wipe the block guard
    public static String BLOCK_CD = "block";

    public static int BASE_BLOCK_COOLDOWN_TICKS = 20;
    public static int MIN_BLOCK_COOLDOWN_TICKS = 5;

    // the cap everyone has before Maximum Block Chance. the stat's own max is the absolute ceiling
    public static float BASE_BLOCK_CAP = 75;

    // Block Recovery is a frequency increase: +100% means the shield is back up twice as fast, so it
    // divides the cooldown instead of subtracting from it
    public static int getBlockCooldownTicks(EntityData target) {
        float recovery = target.getUnit()
                .getCalculatedStat(BlockRecovery.getInstance())
                .getValue();

        return Math.max(MIN_BLOCK_COOLDOWN_TICKS,
                Math.round(BASE_BLOCK_COOLDOWN_TICKS / (1F + Math.max(-99F, recovery) / 100F)));
    }

    // getValueOrBase, not getValue - Stat.base is only applied by InCalcStatData once some source
    // touches the stat, and a character with nothing modifying this has no entry at all
    public static float getBlockDamageReduction(EntityData target) {
        BlockDamageReduction stat = BlockDamageReduction.getInstance();
        return target.getUnit()
                .getCalculatedStat(stat)
                .getValueOrBase(stat);
    }

    public static BlockChance getInstance() {
        return BlockChance.SingletonHolder.INSTANCE;
    }

    @Override
    public String locDescForLangFile() {
        return "Chance to passively block a hit, stopping Block Damage Reduction percent of it. Requires a shield in offhand. Blocked hits can't proc ailments like burn. Blocking has a 1 second cooldown, shortened by Block Recovery.";
    }

    private BlockChance() {
        this.min = 0;
        // the absolute ceiling. the cap that actually gates the roll is BASE_BLOCK_CAP plus
        // Maximum Block Chance, applied in getUsableValue - same split ElementalResist uses
        this.max = 90;
        this.group = StatGroup.MAIN;

        this.statEffect = new BlockChance.Effect();

        this.format = ChatFormatting.BLUE.getName();

    }

    @Override
    public float getAdditionalMax(Unit data) {
        return data.getCalculatedStat(MaxBlockChance.getInstance())
                .getValue();
    }

    @Override
    public float getUsableValue(Unit unit, int value, int lvl) {
        return MathHelper.clamp(value, min, BASE_BLOCK_CAP + getAdditionalMax(unit)) / 100F;
    }

    @Override
    public float getMaxMulti() {
        return 0;
    }

    @Override
    public float valueNeededToReachMaximumPercentAtLevelOne() {
        return 0;
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
        return true;
    }

    @Override
    public String locNameForLangFile() {
        return "Block Chance";
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
            float reduction = getBlockDamageReduction(effect.targetData);

            // a bonus element split of an already blocked hit inherits the block instead of
            // rolling its own
            if (isInheritedBlock(effect)) {
                applyBlock(effect, reduction);
                return effect;
            }

            float chance = ((BlockChance) stat).getUsableValue(effect.targetData.getUnit(),
                    (int) data.getValue(), effect.targetData.getLevel()) * 100F;

            if (RandomUtils.roll(chance)) {
                applyBlock(effect, reduction);
                effect.targetData.getCooldowns().setOnCooldown(BLOCK_CD, getBlockCooldownTicks(effect.targetData));

                // cooldowns are set server side and only ticked down on the client, so the client never
                // learns about this one on its own - and EffectsOverlay draws the block_disabled icon off it.
                // same reason SpellCastingData marks dirty after stamping a spell cooldown
                effect.targetData.sync.setDirty();
            }
            return effect;
        }

        private static void applyBlock(DamageEvent effect, float reduction) {
            if (reduction >= 100) {
                // a full block negates the hit outright, the same way dodging does. this also stops
                // the whole bonus element sweep before it starts (see DamageEvent.activate), so no
                // converted or "taken as" part of the hit can leak through
                effect.data.setHitAvoided(EventData.IS_BLOCKED);
            } else {
                // reduce(35) leaves a x0.65 multiplier - the target eats the other 65%
                effect.getLayer(StatLayers.Defensive.DAMAGE_BLOCK, EventData.NUMBER, EffectSides.Target)
                        .reduce(reduction);
                effect.data.setBoolean(EventData.IS_BLOCKED, true);
            }
        }

        private static boolean isInheritedBlock(DamageEvent effect) {
            return effect.getAttackType() == AttackType.bonus_dmg
                    && effect.data.getBoolean(EventData.IS_BLOCKED);
        }

        @Override
        public boolean canActivate(DamageEvent effect, StatData data, Stat stat) {
            if (!effect.canAvoidHit()) {
                return false;
            }
            // the parent hit already rolled and already paid the cooldown, its element splits just
            // inherit the answer
            if (isInheritedBlock(effect)) {
                return true;
            }
            // dodge and the raised shield resolve as avoidance too. don't spend the block cooldown
            // on a hit that was already going to deal nothing
            if (effect.data.isHitAvoided()) {
                return false;
            }
            if (effect.targetData.getCooldowns().isOnCooldown(BLOCK_CD)) {
                return false;
            }
            // the shield is the whole premise of passive block, and Block Chance also comes from
            // perks, ascendancies and aura gems - not just from the shield itself. written as its
            // own guard because `a || b && c` only ever gated the bonus_dmg branch, which let a
            // perk-sourced block roll with a tome or totem in the offhand
            if (!(effect.target.getOffhandItem().getItem() instanceof ShieldItem)) {
                return false;
            }
            return effect.getAttackType().isHit() || effect.getAttackType() == AttackType.bonus_dmg;
        }
    }

    private static class SingletonHolder {
        private static final BlockChance INSTANCE = new BlockChance();
    }
}
