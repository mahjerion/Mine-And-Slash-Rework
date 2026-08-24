package com.robertx22.mine_and_slash.database.data.spells.components;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.IGUID;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.tooltip.ExileTooltipUtils;
import com.robertx22.library_of_exile.util.UNICODE;
import com.robertx22.library_of_exile.wrappers.ExileText;
import com.robertx22.mine_and_slash.a_libraries.player_animations.AnimationHolder;
import com.robertx22.mine_and_slash.a_libraries.player_animations.PlayerAnimations;
import com.robertx22.mine_and_slash.a_libraries.player_animations.SpellAnimations;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SpellDesc;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SummonPetAction;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.database.data.stats.tooltips.SummonTooltip;
import com.robertx22.mine_and_slash.database.data.value_calc.MaxLevelProvider;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRangeInfo;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.ISkillGem;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.tags.imp.SpellTag;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.SpendResourceEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.PlayStyle;
import com.robertx22.mine_and_slash.uncommon.enumclasses.WeaponTypes;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocDesc;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import com.robertx22.mine_and_slash.uncommon.interfaces.IWikiHideable;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import com.robertx22.mine_and_slash.uncommon.localization.Itemtips;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.MapManager;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.TooltipUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public final class Spell implements ISkillGem, IGUID, IAutoGson<Spell>, JsonExileRegistry<Spell>, IAutoLocName, IAutoLocDesc, MaxLevelProvider, IWikiHideable {
    public static Spell SERIALIZER = new Spell();

    public static String DEFAULT_EN_NAME = "default_entity_name";
    public static String CASTER_NAME = "caster";

    // fastest a channel may pulse, 10 pulses a second
    public static final int MIN_CHANNEL_PULSE_TICKS = 1;

    public int weight = 1000;
    public String identifier = "";
    public Boolean hide_from_wiki = null;

    @Override
    public Boolean getHideFromWiki() {
        return hide_from_wiki;
    }

    public AttachedSpell attached = new AttachedSpell();
    public SpellConfiguration config = new SpellConfiguration();

    public SpellAnimationData cast_animation = new SpellAnimationData(SpellAnimations.STEADY_CAST);
    public SpellAnimationData cast_finish_animation = new SpellAnimationData(SpellAnimations.CAST_FINISH);

    public Boolean hasCost(ResourceType type) {
        if (type == ResourceType.energy) {
            return config.ene_cost.min > 0;
        }
        if (type == ResourceType.mana) {
            return config.mana_cost.min > 0;
        }
        return null;
    }

    public int min_lvl = 1;
    public int default_lvl = 0;
    public String lvl_based_on_spell = "";
    public String show_other_spell_tooltip = "";
    public boolean manual_tip = false;
    public List<String> disabled_dims = new ArrayList<>();
    public String effect_tip = "";

    // todo make this a tag instead of using a negative tag maybe?
    public boolean usesWeaponForDamage() {
        //changed to always be true instead of deleting condition for readability. should be true because staff damage is useless otherwise
        return true;
        //return !config.tags.contains(SpellTags.magic);
    }

    public int max_lvl = 20; // first lvl unlocks spell, then every 4 lvls unlocks a supp gem slot
    public List<StatMod> statsForSkillGem = new ArrayList<>();
    public transient String locDesc = "";

    public boolean isAllowedInDimension(Level world) {
        if (disabled_dims.isEmpty()) {
            return true;
        }
        return disabled_dims.stream()
                .map(x -> new ResourceLocation(x))
                .noneMatch(x -> x.equals(MapManager.getResourceLocation(world)));
    }

    public AttachedSpell getAttached() {
        return attached;
    }

    public List<ExactStatData> getStats(Player p) {
        int perc = (int) ((getLevelOf(p) / (float) getMaxLevelWithBonuses()) * 100F);
        var stats = statsForSkillGem.stream().map(x -> x.ToExactStat(perc, Load.Unit(p).getLevel())).collect(Collectors.toList());
        return stats;
    }

    public boolean is(SpellTag tag) {
        return config.tags.contains(tag);
    }

    public SpellConfiguration getConfig() {
        return config;
    }

    public void validate() {
        for (ComponentPart x : this.attached.getAllComponents()) {
            x.validate();
        }
    }

    public final ResourceLocation getIconLoc() {
        return getIconLoc(GUID());
    }

    public static final ResourceLocation getIconLoc(String id) {
        return new ResourceLocation(SlashRef.MODID, "textures/gui/spells/icons/" + id + ".png");
    }

    public WeaponTypes getWeapon(LivingEntity en) {
        try {
            if (getStyle() != PlayStyle.INT) {
                var wep = Load.Unit(en).equipmentCache.getWeapon();
                if (wep != null && wep.gear != null) {
                    return wep.gear.GetBaseGearType().weaponType();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return WeaponTypes.none;
    }

    public final void onCastingTick(SpellCastContext ctx) {
        int timesToCast = (int) ctx.spell.getConfig().times_to_cast;
        if (timesToCast > 1) {
            // check how many times we should've cast by now to see if it increased
            int castTimeTicks = getCastTimeTicks(ctx);
            int castCountLastTick = (ctx.ticksInUse - 1) * timesToCast / castTimeTicks;
            int castCountThisTick = ctx.ticksInUse * timesToCast / castTimeTicks;

            if (castCountThisTick != castCountLastTick) {
                this.cast(ctx);
            }
        } else if (timesToCast < 1) {
            ExileLog.get().warn("Times to cast spell is: " + timesToCast + " . this seems like a bug.");
        }
    }

    public void cast(SpellCastContext ctx) {
        LivingEntity caster = ctx.caster;
        ctx.castedThisTick = true;
        /*
        if (MMORPG.RUN_DEV_TOOLS_REMOVE_WHEN_DONE && this.config.swing_arm) {
            //    caster.swingTime = -1; // this makes sure hand swings
            //   caster.swing(InteractionHand.MAIN_HAND);
        }
        */
        attached.onCast(SpellCtx.onCast(caster, ctx.calcData));
    }

    public final int getCooldownTicks(SpellCastContext ctx) {
        return (int) Math.ceil(ctx.event.data.getNumber(EventData.COOLDOWN_TICKS).number);
    }


    // the recovery this cast imposes, on this skill and on every other one. already scaled by the
    // cast/attack/channel speed stats, so read it from the event and not from config
    public final int getCastSpeedTicks(SpellCastContext ctx) {
        return (int) Math.ceil(ctx.event.data.getNumber(EventData.CAST_SPEED_TICKS).number);
    }

    // a skill is never ready again before its own recovery is over, even when its cooldown is shorter.
    // every path that stamps a spell's own cooldown goes through here so they cannot drift apart
    public final int getEffectiveCooldownTicks(SpellCastContext ctx) {
        return Math.max(getCooldownTicks(ctx), getCastSpeedTicks(ctx));
    }

    public final int getChargeCooldownTicks(SpellCastContext ctx) {
        return (int) Math.ceil(ctx.event.data.getNumber(EventData.CHARGE_COOLDOWN_TICKS).number);
    }

    public final int getCastTimeTicks(SpellCastContext ctx) {
        int ticks = (int) Math.ceil(ctx.event.data.getNumber(EventData.CAST_TICKS).number);
        if (config.isChannel()) {
            // for a channel this is the gap between pulses, so cap the rate instead. without a floor
            // stacked channel cast speed reaches one pulse per tick.
            return MathHelper.clamp(ticks, MIN_CHANNEL_PULSE_TICKS, 10000);
        }
        // if it casts 5 times a cast, it should take at least 5 ticks to cast it
        return MathHelper.clamp(ticks, config.times_to_cast, 10000);
    }

    @Override
    public String GUID() {
        return identifier;
    }

    public void spendResources(SpellCastContext ctx) {
        getManaCostCtx(ctx).Activate();
        getEnergyCostCtx(ctx).Activate();
    }

    public SpendResourceEvent getManaCostCtx(SpellCastContext ctx) {
        float cost = this.getCalculatedManaCost(ctx);
        SpendResourceEvent event = new SpendResourceEvent(ctx.caster, this, ResourceType.mana, cost);
        event.calculateEffects();
        return event;
    }

    public SpendResourceEvent getEnergyCostCtx(SpellCastContext ctx) {
        float cost = this.getCalculatedEnergyCost(ctx);
        SpendResourceEvent event = new SpendResourceEvent(ctx.caster, this, ResourceType.energy, cost);
        event.calculateEffects();
        return event;
    }

    public final int getCalculatedManaCost(SpellCastContext ctx) {
        return channelCostFloor((int) ctx.event.data.getNumber(EventData.MANA_COST).number, config.mana_cost.min);
    }

    public final int getCalculatedEnergyCost(SpellCastContext ctx) {
        return channelCostFloor((int) ctx.event.data.getNumber(EventData.ENERGY_COST).number, config.ene_cost.min);
    }

    // a channel pays this on every pulse, and per pulse costs are small enough that cost reductions
    // plus the int truncation can round one down to a free channel. anything that costs at all costs 1.
    private int channelCostFloor(int cost, float configured) {
        if (config.isChannel() && configured > 0) {
            return Math.max(1, cost);
        }
        return cost;
    }

    // Helper record to pair effects and durations
    private static class EffectWithDuration {
        public final ExileEffect effect;
        public final String duration;

        public EffectWithDuration(ExileEffect effect, String duration) {
            this.effect = effect;
            this.duration = duration;
        }
    }

    public final List<Component> GetTooltipString(StatRangeInfo info) {
        SpellCastContext ctx = new SpellCastContext(info.player, 0, this);
        List<Component> list = new ArrayList<>();
        list.add(locName().withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        list.add(ExileText.emptyLine().get());

        if (true || Screen.hasShiftDown()) {
            SpellDesc.getTooltip(ctx.caster, this)
                    .forEach(x -> list.add(Component.literal(x)));
        }

        list.add(ExileText.emptyLine().get());

        int mana = getCalculatedManaCost(ctx);
        int ene = getCalculatedEnergyCost(ctx);

        if (mana > 0) {
            list.add(Words.MANA_COST.locName(mana).withStyle(ChatFormatting.BLUE));
        }
        if (ene > 0) {
            list.add(Words.ENE_COST.locName(ene).withStyle(ChatFormatting.GREEN));
        }
        int recovery = getCastSpeedTicks(ctx);

        if (config.usesCharges()) {
            list.add(Words.MAX_CHARGES.locName(config.charges).withStyle(ChatFormatting.YELLOW));
            // read from the event, not from config - Cooldown Reduction shortens charge regen too, and
            // getChargeCooldownTicks is the same value spendCharge actually stores on the charge
            list.add(Words.CHARGE_REGEN.locName(tooltipFormatTicksAsSeconds(getChargeCooldownTicks(ctx))).withStyle(ChatFormatting.YELLOW));
        } else if (getCooldownTicks(ctx) > recovery) {
            // recovery is the floor of every skill's cycle now, so only a genuinely longer cooldown
            // is worth a line. printing both on the ~250 skills where they match is just noise
            list.add(Words.COOLDOWN.locName(tooltipFormatTicksAsSeconds(getCooldownTicks(ctx))).withStyle(ChatFormatting.YELLOW));
        }

        list.add(Words.RECOVERY.locName(tooltipFormatTicksAsSeconds(recovery)).withStyle(ChatFormatting.YELLOW));

        int casttime = getCastTimeTicks(ctx);

        if (config.isChannel()) {
            // a channel has no cast time, casttime is the gap between pulses
            list.add(Words.CHANNEL_PULSE_RATE.locName(tooltipFormatTicksAsSeconds(casttime)).withStyle(ChatFormatting.GREEN));
        } else if (casttime <= 1) {
            list.add(Words.INSTANT_CAST.locName().withStyle(ChatFormatting.GREEN));
        } else {
            list.add(Words.CAST_TIME.locName(tooltipFormatTicksAsSeconds(casttime)).withStyle(ChatFormatting.GREEN));
        }

        Set<String> radiuses = new LinkedHashSet<>();
        this.getAttached()
                .getAllComponents()
                .forEach(x -> {
                    x.targets.forEach(a -> {
                        // adds radius for damage spells
                        if (x.acts.stream().anyMatch(e -> e.type.equals(SpellAction.DEAL_DAMAGE.GUID())) && a.has(MapField.RADIUS)) {
                            String rad = TOOLTIP_NUMBER_FORMAT.format(a.getOrDefault(MapField.RADIUS, 0D).doubleValue());
                            radiuses.add(rad);
                        }
                    });
                });

        for (String radius : radiuses) {
            list.add(Words.Radius.locName(radius).withStyle(ChatFormatting.GOLD));
        }

        list.add(ExileText.emptyLine().get());

        if (!CompatConfig.get().ignoreWeaponReqForSpells()) {
            list.add(getConfig().castingWeapon.predicate.text);
        }

        list.add(ExileText.emptyLine().get());

        if (this.config.isChannel()) {
            list.add(Words.CHANNELLED.locName().withStyle(ChatFormatting.RED));
            list.add(ExileText.emptyLine().get());
        }

        if (this.config.times_to_cast > 1) {
            list.add(ExileText.emptyLine().get());
            list.add(Words.CASTED_TIMES_CHANNEL.locName(config.times_to_cast).withStyle(ChatFormatting.RED));
        }

        boolean showeffect = Screen.hasShiftDown();

        // --- FIX: Use a map to keep only the effect with the greatest duration ---
        LinkedHashMap<ExileEffect, String> effectsWithDurations = new LinkedHashMap<>();

        if (ExileDB.ExileEffects().isRegistered(effect_tip)) {
            effectsWithDurations.put(
                    ExileDB.ExileEffects().get(effect_tip),
                    "" // No duration for this one, but could be handled if needed
            );
        }

        List<SummonTooltip> summons = new ArrayList<>();

        try {
            this.getAttached()
                    .getAllComponents()
                    .forEach(x -> {
                        x.acts.forEach(a -> {
                            if (a.has(MapField.EXILE_POTION_ID)) {
                                ExileEffect eff = a.getExileEffect();
                                String dur = tooltipFormatTicksAsSeconds((int) (double) a.getOrDefault(MapField.POTION_DURATION, 0D));
                                // If already present, keep the greater duration
                                if (effectsWithDurations.containsKey(eff)) {
                                    String existingDur = effectsWithDurations.get(eff);
                                    float newDur = parseDuration(dur);
                                    float oldDur = parseDuration(existingDur);
                                    if (newDur > oldDur) {
                                        effectsWithDurations.put(eff, dur);
                                    }
                                } else {
                                    effectsWithDurations.put(eff, dur);
                                }
                            }
                            if (a.has(MapField.SUMMONED_PET_ID)) {
                                float duration = a.get(MapField.LIFESPAN_TICKS).intValue();
                                Optional<EntityType<?>> type = EntityType.byString(a.get(MapField.SUMMONED_PET_ID));
                                String name = Words.SUMMON_UNKNOWN_ENTITY.locNameLangFileGUID();
                                if (type.isPresent()) {
                                    name = type.get().getDescriptionId();
                                }
                                String finalName = name;
                                if (summons.stream().filter(i -> i.type().equals(finalName) && i.duration() == duration).findAny().isEmpty()) {
                                    summons.add(new SummonTooltip(name, duration));
                                }
                            }
                            if (a.has(MapField.make(MapField.BLOCK))) {
                                float duration = a.get(MapField.LIFESPAN_TICKS).intValue();
                                String name = this.loc_name;
                                if (summons.stream().filter(i -> i.type().equals(name) && i.duration() == duration).findAny().isEmpty()) {
                                    summons.add(new SummonTooltip(name, duration));
                                }
                            }
                        });
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        MutableComponent showEffectTip = null;
        try {
            if (showeffect) {
                for (Map.Entry<ExileEffect, String> entry : effectsWithDurations.entrySet()) {
                    ExileEffect effect = entry.getKey();
                    String duration = entry.getValue();
                    list.add(effect.locName().withStyle(ChatFormatting.BLUE));
                    list.add(effect.max_stacks > 1 ? Words.Stats.locName().append(Words.PER_STACK.locName()).withStyle(ChatFormatting.GREEN) : Words.Stats.locName().withStyle(ChatFormatting.GREEN));
                    List<ExactStatData> stats = effect.getExactStats(ctx.caster, this, 1, 1);
                    for (ExactStatData stat : stats) {
                        list.addAll(stat.GetTooltipString());
                    }
                    if (duration != null && !duration.isEmpty()) {
                        list.add(Words.LASTS_SEC.locName(duration));
                    }
                    list.add(ExileText.emptyLine().get());
                }
            } else {
                if (!effectsWithDurations.isEmpty()) {
                    showEffectTip = Words.SHIFT_TO_SHOW_EFFECT.locName().withStyle(ChatFormatting.BLUE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!summons.isEmpty()) {
            list.add(Words.SUMMONS_LIST.locName());
        }
        for (SummonTooltip summon : summons) {
            list.add(Component.translatable(summon.type()).withStyle(ChatFormatting.BLUE));

            MutableComponent durationValue = Component.literal(String.valueOf(summon.duration() / 20));
            if (summon.duration() == SummonPetAction.INFINITE_DURATION) {
                durationValue = Words.SUMMON_IS_INFINITE.locName();
            }

            list.add(durationValue.withStyle(ChatFormatting.GREEN)
                    .append(Words.UNIT_SECOND.locName()).append(" ")
                    .append(Words.SUMMON_BASE_DURATION.locName().withStyle(ChatFormatting.GRAY))
            );
            list.add(ExileText.emptyLine().get());
        }

        if (!this.statsForSkillGem.isEmpty()) {
            list.add(Words.SPELL_STATS.locName());
            for (ExactStatData stat : getStats(info.player)) {
                list.addAll(stat.GetTooltipString());
            }
        }

        if (info.hasShiftDown) {
            list.add(Words.PROC_RECHARGE.locName(tooltipFormatTicksAsSeconds(config.proc_cooldown_ticks))
                    .withStyle(ChatFormatting.GRAY));
        }

        MutableComponent taglist = ExileTooltipUtils.joinMutableComps(this.config.tags.getTags(SpellTag.SERIALIZER).stream().map(x -> x.locName()).iterator(), Gui.COMMA_SEPARATOR.locName());
        MutableComponent tagtext = Words.TAGS.locName().append(taglist);

        list.add(tagtext);

        if (showEffectTip != null) {
            list.add(showEffectTip);
        }

        if (info.hasShiftDown && this.config.tags.contains(SpellTags.has_pet_ability)) {
            list.clear(); // tooltip too long otherwise
            list.add(Words.PET_BASIC.locName());
            Spell spell = config.getSummonBasicSpell();
            list.addAll(spell.GetTooltipString(info));
        }
        if (!this.show_other_spell_tooltip.isEmpty()) {
            if (info.hasAltDown) {
                list.clear(); // tooltip too long otherwise
                Spell other = ExileDB.Spells().get(show_other_spell_tooltip);
                list.addAll(other.GetTooltipString(info));
            } else {
                list.add(Chats.ALT_TO_SHOW_OTHER_SPELL.locName().withStyle(ChatFormatting.BLUE));
            }
        }
        var others = this.getSpellsThatBenefitFromSupportGemsOfThis();

        if (!others.isEmpty()) {
            var merged = ExileTooltipUtils.joinMutableComps(others.stream().map(x -> x.locName()).iterator(), Component.literal(", "));
            list.add(Component.literal(UNICODE.STAR + " ").append(Words.SUPPORT_GEMS_ALSO_BENEFIT.locName(merged)).withStyle(ChatFormatting.GREEN));
        }

        if (showEffectTip == null) {
            list.add(Itemtips.SHIFT_TIP.locName().withStyle(ChatFormatting.BLUE).append(" "));
        }

        TooltipUtils.removeDoubleBlankLines(list);

        return list;
    }

    // Helper to parse duration string to float, treating empty as 0
    private static float parseDuration(String dur) {
        if (dur == null || dur.isEmpty()) return 0f;
        try {
            return Float.parseFloat(dur);
        } catch (Exception e) {
            return 0f;
        }
    }

    private static final DecimalFormat TOOLTIP_NUMBER_FORMAT = new DecimalFormat("0.##");

    private static String tooltipFormatTicksAsSeconds(int ticks) {
        return TOOLTIP_NUMBER_FORMAT.format(ticks / 20F);
    }

    public int getLevelOf(LivingEntity en) {
        if (!lvl_based_on_spell.isEmpty()) {
            var other = ExileDB.Spells().get(lvl_based_on_spell);
            if (!other.lvl_based_on_spell.equals(this.lvl_based_on_spell)) {// just to make sure it doesnt loop forever
                return other.getLevelOf(en);
            }
        }

        int lvl = 0;

        if (en instanceof Player p) {
            // learned spells, not just hotbar ones. otherwise an unslotted spell reads as lvl 0
            lvl = Load.player(p).spellCastingData.getSpellData(GUID()).rank;
        }
        if (lvl < default_lvl) {
            lvl = default_lvl;
        }

        return lvl;
    }

    public List<Spell> getSpellsThatBenefitFromSupportGemsOfThis() {
        return ExileDB.Spells().getFilterWrapped(x -> x.config.use_support_gems_from.equals(GUID())).list;
    }

    // todo need to make my own animations
    public AnimationHolder getAnimation(PlayerAnimations.CastEnum e) {
        if (e == PlayerAnimations.CastEnum.CAST_START) {
            if (false) {
                return SpellAnimations.SPIN;
            }
            return this.cast_animation.getAnim();
        }
        if (e == PlayerAnimations.CastEnum.CAST_FINISH) {
            if (false) {
                return SpellAnimations.CAST_FINISH;
            }
            return this.cast_finish_animation.getAnim();
        }
        return AnimationHolder.none();
    }

    @Override
    public Class<Spell> getClassForSerialization() {
        return Spell.class;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.SPELL;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Spells;
    }

    @Override
    public int Weight() {
        return weight;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".spell." + GUID();
    }

    public String loc_name;

    @Override
    public String locNameForLangFile() {
        return loc_name;
    }

    @Override
    public AutoLocGroup locDescGroup() {
        return AutoLocGroup.Spells;
    }

    @Override
    public String locDescLangFileGUID() {
        return "spell.desc." + GUID();
    }

    @Override
    public String locDescForLangFile() {
        return locDesc;
    }

    @Override
    public int getCurrentLevel(LivingEntity en) {
        return getLevelOf(en);
    }

    @Override
    public int getMaxLevel() {
        return max_lvl;
    }

    @Override
    public int getMaxLevelWithBonuses() {
        return getMaxLevel() + GameBalanceConfig.get().MAX_BONUS_SPELL_LEVELS;
    }

    @Override
    public int getRequiredLevel() {
        return min_lvl;
    }

    @Override
    public PlayStyle getStyle() {
        return this.config.getStyle();
    }
}