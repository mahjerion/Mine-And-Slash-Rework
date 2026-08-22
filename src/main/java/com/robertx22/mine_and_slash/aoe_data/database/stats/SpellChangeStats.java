package com.robertx22.mine_and_slash.aoe_data.database.stats;

import com.robertx22.mine_and_slash.aoe_data.database.stat_conditions.StatConditions;
import com.robertx22.mine_and_slash.aoe_data.database.stat_effects.StatEffects;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SummonType;
import com.robertx22.mine_and_slash.aoe_data.database.stats.base.DatapackStatBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.stats.base.EmptyAccessor;
import com.robertx22.mine_and_slash.database.data.aura.AuraGems;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatScaling;
import com.robertx22.mine_and_slash.database.data.stats.datapacks.test.DataPackStatAccessor;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.tags.imp.SpellTag;
import com.robertx22.mine_and_slash.uncommon.effectdatas.GenerateThreatEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.SpellStatsCalculationEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.ThreatGenType;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import net.minecraft.ChatFormatting;

public class SpellChangeStats {
    public static DataPackStatAccessor<AuraGems.AuraInfo> SPECIFIC_AURA_COST = DatapackStatBuilder
            .<AuraGems.AuraInfo>of(x -> x.id + "_aura_cost", x -> Elements.Physical)
            .addAllOfType(AuraGems.ALL)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .setLocName(x -> Stat.format(x.name + " Augment Cost"))
            .setLocDesc(x -> "Reduces reservation cost of Augments, potentially allowing you to equip more.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.minus_is_good = true;
            })
            .build();
    public static DataPackStatAccessor<SummonType> MAX_SUMMONS_PER_TYPE = DatapackStatBuilder
            .<SummonType>of(x -> "max_" + x.id + "_summons", x -> Elements.ALL)
            .addAllOfType(SummonType.getCapped())
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(x -> StatConditions.IS_SUMMON_TYPE.get(x))
            .addEffect(StatEffects.ADD_TOTAL_SUMMONS)
            .setLocName(x -> "Maximum " + x.name + " Summons")
            .setLocDesc(x -> "You can have more " + x.name + " minions active at once.")
            .modifyAfterDone(x ->
            {
                x.is_perc = false;
                x.base = 0;
                x.min = 0;
                x.max = 20;
            }).
            build();
    public static DataPackStatAccessor MAX_TOTEM_CAPACITY = DatapackStatBuilder
            .ofSingle("max_totems", Elements.ALL)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addEffect(StatEffects.ADD_MAX_TOTEMS)
            .setLocName(x -> "Maximum Totems")
            .setLocDesc(x -> "You can have more Totems active at once.")
            .modifyAfterDone(x ->
            {
                x.is_perc = false;
                x.base = 0;
                x.min = 0;
                x.max = 10;
            }).
            build();
    public static DataPackStatAccessor TOTEM_COUNT = DatapackStatBuilder
            .ofSingle("totem_count", Elements.ALL)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addEffect(StatEffects.ADD_TOTEM_COUNT)
            .setLocName(x -> "Totems Per Cast")
            .setLocDesc(x -> "Each Totem Skill places extra totems at once, up to your Maximum Totems.")
            .modifyAfterDone(x ->
            {
                x.is_perc = false;
                x.base = 0;
                x.min = 0;
                x.max = 10;
            }).
            build();
    public static DataPackStatAccessor MAX_BANNER_CAPACITY = DatapackStatBuilder
            .ofSingle("max_banners", Elements.ALL)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addEffect(StatEffects.ADD_MAX_BANNERS)
            .setLocName(x -> "Maximum Banners")
            .setLocDesc(x -> "You can have more Banners planted at once.")
            .modifyAfterDone(x ->
            {
                x.is_perc = false;
                x.base = 0;
                x.min = 0;
                x.max = 5;
            }).
            build();
    public static DataPackStatAccessor<EmptyAccessor> MANA_COST = DatapackStatBuilder
            .ofSingle("mana_cost", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Damage.AFTER_DAMAGE_BONUSES)
            .setSide(EffectSides.Source)
            .addEffect(StatEffects.INCREASE_MANA_COST)
            .setLocName(x -> "Mana Cost")
            .setLocDesc(x -> "Modifies mana cost of Skills.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.base = 0;
                x.min = -75;
                x.max = 300;
                x.minus_is_good = true;
            })
            .build();

    // every speed stat sums into one accumulator that SpellStatsCalculationEvent divides by, so they
    // read as "how often", never as a flat cut off the timer. the archetype split is by PlayStyle:
    // SpellBuilder gives every skill its style tag, so magic == Int. Channels are not excluded here -
    // activate() weights the whole accumulator down for them instead, which is what lets a per tag
    // roll like Fire Skill Speed reach a channelled fire skill.
    public static DataPackStatAccessor<EmptyAccessor> SKILL_SPEED = DatapackStatBuilder
            .ofSingle("skill_speed", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(x -> StatConditions.SPELL_NOT_HAVE_TAG.get(SpellTags.not_affected_by_cast_speed))
            .addEffect(StatEffects.ADD_CAST_SPEED_PERCENT)
            .setLocName(x -> "Skill Speed")
            .setLocDesc(x -> "How often you can use any Skill. Shortens Cast Time and Recovery, but not Cooldowns. +100% means twice as many casts. Channelled Skills gain half as much.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.base = 0;
                x.min = -75;
                x.max = 300;
            })
            .build();
    public static DataPackStatAccessor<EmptyAccessor> CAST_SPEED = DatapackStatBuilder
            .ofSingle("cast_speed", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(x -> StatConditions.SPELL_HAS_TAG.get(SpellTags.magic))
            .addCondition(x -> StatConditions.SPELL_NOT_HAVE_TAG.get(SpellTags.not_affected_by_cast_speed))
            .addEffect(StatEffects.ADD_CAST_SPEED_PERCENT)
            .setLocName(x -> "Magic Skill Speed")
            .setLocDesc(x -> "How often you can use Magic Skills. Shortens Cast Time and Recovery, but not Cooldowns. +100% means twice as many casts. Channelled Skills gain half as much.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.base = 0;
                x.min = -75;
                x.max = 300;
            })
            .build();
    public static DataPackStatAccessor<EmptyAccessor> ATTACK_CAST_SPEED = DatapackStatBuilder
            .ofSingle("attack_cast_speed", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.IS_ATTACK_DAMAGE)
            .addCondition(x -> StatConditions.SPELL_NOT_HAVE_TAG.get(SpellTags.not_affected_by_cast_speed))
            .addEffect(StatEffects.ADD_CAST_SPEED_PERCENT)
            .setLocName(x -> "Attack Skill Speed")
            .setLocDesc(x -> "How often you can use Melee and Ranged Skills. Shortens Cast Time and Recovery, but not Cooldowns. +100% means twice as many casts. Channelled Skills gain half as much.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.base = 0;
                x.min = -75;
                x.max = 300;
            })
            .build();
    // the only stat a channel gets at full weight, and the only one that does nothing anywhere else
    public static DataPackStatAccessor<EmptyAccessor> CHANNEL_SPEED = DatapackStatBuilder
            .ofSingle("channel_speed", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(x -> StatConditions.SPELL_HAS_TAG.get(SpellTags.channel))
            .addCondition(x -> StatConditions.SPELL_NOT_HAVE_TAG.get(SpellTags.not_affected_by_cast_speed))
            .addEffect(StatEffects.ADD_CHANNEL_SPEED_PERCENT)
            .setLocName(x -> "Channel Speed")
            .setLocDesc(x -> "How fast Channelled Skills pulse while you hold the key. Shortens the gap between pulses and your Recovery, but not Cooldowns. +100% means twice as many pulses.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.base = 0;
                x.min = -75;
                x.max = 300;
            })
            .build();
    public static DataPackStatAccessor<SpellTag> CAST_TIME_PER_SPELL_TAG = DatapackStatBuilder
            .<SpellTag>of(x -> x.GUID() + "_cast_time", x -> Elements.Physical)
            .addAllOfType(SpellTag.getAll())

            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(x -> StatConditions.SPELL_HAS_TAG.get(x))
            .addCondition(x -> StatConditions.SPELL_NOT_HAVE_TAG.get(SpellTags.not_affected_by_cast_speed))
            .addEffect(StatEffects.ADD_CAST_SPEED_PERCENT)
            .setLocName(x -> x.locNameForLangFile() + " Skill Speed")
            .setLocDesc(x -> "How often you can use Skills with this tag. Shortens Cast Time and Recovery, but not Cooldowns. +100% means twice as many casts.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.base = 0;
                x.min = -75;
                x.max = 300;
            })
            .build();
    public static DataPackStatAccessor<SpellTag> COOLDOWN_REDUCTION_PER_SPELL_TAG = DatapackStatBuilder
            .<SpellTag>of(x -> x.GUID() + "_cdr", x -> Elements.Physical)
            .addAllOfType(SpellTag.getAll())
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(x -> StatConditions.SPELL_HAS_TAG.get(x))
            .addEffect(StatEffects.DECREASE_COOLDOWN)
            .addEffect(StatEffects.DECREASE_CHARGE_CD)
            .setLocName(x -> x.locNameForLangFile() + " Skill Cooldown Reduction")
            .setLocDesc(x -> "Reduces Cooldowns and Charge Regeneration of Skills with the tag. Does not affect Cast Time or Recovery. Only matters on Skills whose Cooldown is longer than their Recovery.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.base = 0;
                x.max = 50;
            })
            .build();
    public static DataPackStatAccessor<EmptyAccessor> COOLDOWN_REDUCTION = DatapackStatBuilder
            .ofSingle("cdr", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addEffect(StatEffects.DECREASE_COOLDOWN)
            .addEffect(StatEffects.DECREASE_CHARGE_CD)
            .setLocName(x -> "Cooldown Reduction")
            .setLocDesc(x -> "Reduces Skill Cooldowns and Charge Regeneration. Does not affect Cast Time or Recovery. Only matters on Skills whose Cooldown is longer than their Recovery.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.base = 0;
                x.max = 75;
            })
            .build();
    public static DataPackStatAccessor<EmptyAccessor> COOLDOWN_TICKS = DatapackStatBuilder
            .ofSingle("cd_ticks", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addEffect(StatEffects.DECREASE_COOLDOWN_BY_X_TICKS)
            .setLocName(x -> "Cooldown Ticks")
            .setLocDesc(x -> "Reduces Skill cooldown by x ticks.")
            .modifyAfterDone(x -> {
                x.is_perc = false;
                x.min = -15;
                x.max = 10000;
            })
            .build();
    public static DataPackStatAccessor<EmptyAccessor> PROJECTILE_SPEED = DatapackStatBuilder
            .ofSingle("faster_projectiles", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.projectile))
            .addEffect(StatEffects.INCREASE_PROJ_SPEED)
            .setLocName(x -> "Projectile Speed")
            .setLocDesc(x -> "Makes your Skill projectiles faster.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.icon = "\u27B9";
                x.format = ChatFormatting.GREEN.getName();
            })
            .build();

    public static DataPackStatAccessor<EmptyAccessor> PROJECTILE_SPREAD_RANDOMNESS = DatapackStatBuilder
            .ofSingle("proj_spread_randomness", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.projectile))
            .addEffect(StatEffects.INCREASE_PROJ_SPREAD_RANDOMNESS)
            .setLocName(x -> "Random Projectile Spread")
            .setLocDesc(x -> "Makes your projectiles fire with randomized pitch and yaw.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.min = -100;
                x.max = 100;
                x.minus_is_good = true;
                x.icon = "\u27B9";
                x.format = ChatFormatting.GREEN.getName();
            })
            .build();

    public static DataPackStatAccessor<EmptyAccessor> PROJECTILE_COUNT = DatapackStatBuilder
            .ofSingle("projectile_count", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.projectile))
            .addEffect(StatEffects.PROJECTILE_COUNT)
            .setLocName(x -> "Projectile Count")
            .setLocDesc(x -> "Adds more projectiles to your Projectile Skills.")
            .modifyAfterDone(x -> {
                x.is_perc = false;
            })
            .build();

    public static DataPackStatAccessor<EmptyAccessor> CHAIN_COUNT = DatapackStatBuilder
            .ofSingle("chain_count", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.chaining))
            .addEffect(StatEffects.BONUS_CHAINS)
            .setLocName(x -> "Chain Count")
            .setLocDesc(x -> "Increases the number of bounces on Skills that chain.")
            .modifyAfterDone(x -> {
                x.is_perc = false;
            })
            .build();

    public static DataPackStatAccessor<EmptyAccessor> PROJECTILE_BARRAGE = DatapackStatBuilder
            .ofSingle("projectile_barrage", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.projectile))
            .addEffect(StatEffects.SET_BOOLEAN.get(EventData.BARRAGE))
            .setLocName(x -> "Projectiles Barrage")
            .setLocDesc(x -> "Causes your projectiles to shoot forward.")
            .modifyAfterDone(x -> {
                x.is_perc = false;
            })
            .build();

    public static DataPackStatAccessor<EmptyAccessor> PROJECTILE_NOVA = DatapackStatBuilder
            .ofSingle("projectile_nova", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.projectile))
            .addEffect(StatEffects.SET_BOOLEAN.get(EventData.NOVA))
            .setLocName(x -> "Projectile Nova")
            .setLocDesc(x -> "Causes your projectiles to fire in a circle around you.")
            .modifyAfterDone(x -> {
                x.is_perc = false;
            })
            .build();

    // todo merge this into duration per spell tag
    public static DataPackStatAccessor<EmptyAccessor> SUMMON_DURATION = DatapackStatBuilder
            .ofSingle("summon_duration", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.summon))
            .addEffect(StatEffects.DURATION_INCREASE)
            .setLocName(x -> "Summon Duration")
            .setLocDesc(x -> "Your summons last longer (mobs like zombie, wolf etc summons)")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.icon = "\u27B9";
                x.format = ChatFormatting.GREEN.getName();
            })

            .build();
    public static DataPackStatAccessor<EmptyAccessor> TOTEM_DURATION = DatapackStatBuilder
            .ofSingle("totem_duration", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.totem))
            .addEffect(StatEffects.DURATION_INCREASE)
            .setLocName(x -> "Totem Duration")
            .setLocDesc(x -> "Increases the duration of your Totems.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.format = ChatFormatting.GREEN.getName();
            })

            .build();
    public static DataPackStatAccessor<EmptyAccessor> BANNER_DURATION = DatapackStatBuilder
            .ofSingle("banner_duration", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.banner))
            .addEffect(StatEffects.DURATION_INCREASE)
            .setLocName(x -> "Banner Duration")
            .setLocDesc(x -> "Increases the duration of your Banners.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.format = ChatFormatting.GREEN.getName();
            })

            .build();
    public static DataPackStatAccessor<EmptyAccessor> AGGRO_RADIUS = DatapackStatBuilder
            .ofSingle("aggro_radius", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.summon))
            .addEffect(StatEffects.AGGRO_INCREASE)
            .setLocName(x -> "Minion Aggro Radius")
            .setLocDesc(x -> "Higher radius means minions can travel further to kill stuff for you, lower means they will stay nearby more")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.format = ChatFormatting.GREEN.getName();
            })

            .build();
    public static DataPackStatAccessor<EmptyAccessor> INCREASED_AREA = DatapackStatBuilder
            .ofSingle("inc_aoe", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.area))
            .addEffect(StatEffects.INCREASE_AREA)
            .setLocName(x -> "Area of Effect")
            .setLocDesc(x -> "Spell aoe effects will be larger")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.max = 100;
            })
            .build();
    public static DataPackStatAccessor<EmptyAccessor> PIERCING_PROJECTILES = DatapackStatBuilder
            .ofSingle("piercing_projectiles", Elements.Physical)
            .worksWithEvent(SpellStatsCalculationEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.SPELL_HAS_TAG.get(SpellTags.projectile))
            .addEffect(StatEffects.SET_BOOLEAN.get(EventData.PIERCE))
            .setLocName(x -> "Piercing Projectiles")
            .setLocDesc(x -> "Makes Projectile Skills pierce enemies.")
            .modifyAfterDone(x -> {
                x.is_perc = false;
                x.is_long = true;
            })
            .build();
    public static DataPackStatAccessor<EmptyAccessor> THREAT_GENERATED = DatapackStatBuilder
            .ofSingle("threat_generated", Elements.Physical)
            .worksWithEvent(GenerateThreatEvent.ID)
            .setPriority(StatPriority.Spell.FIRST)
            .setSide(EffectSides.Source)
            .addEffect(StatEffects.Layers.ADDITIVE_DAMAGE_PERCENT)
            .setLocName(x -> "Threat Generated")
            .setLocDesc(x -> "Modifies amount of threat you generate. Mobs attack targets with highest threat.")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.scaling = StatScaling.NONE;
            })
            .build();
    public static DataPackStatAccessor<EmptyAccessor> MORE_THREAT_WHEN_TAKING_DAMAGE = DatapackStatBuilder
            .ofSingle("more_threat_on_take_dmg", Elements.Physical)
            .worksWithEvent(GenerateThreatEvent.ID)
            .setPriority(StatPriority.Damage.DAMAGE_LAYERS)
            .setSide(EffectSides.Source)
            .addCondition(StatConditions.IS_THREAT_GEN_TYPE.get(ThreatGenType.take_dmg))
            .addEffect(StatEffects.Layers.ADDITIVE_DAMAGE_PERCENT)
            .setLocName(x -> Stat.format("You generate " + Stat.VAL1 + "% more threat when taking damage."))
            .setLocDesc(x -> "")
            .modifyAfterDone(x -> {
                x.is_perc = true;
                x.is_long = true;
                x.scaling = StatScaling.NONE;
            })
            .build();

    public static void init() {

    }
}
