package com.robertx22.mine_and_slash.database.data.spells.components;

import com.robertx22.mine_and_slash.aoe_data.database.spells.SummonType;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.CastingWeapon;
import com.robertx22.mine_and_slash.database.data.value_calc.LeveledValue;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.tags.TagList;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.tags.imp.SpellTag;
import com.robertx22.mine_and_slash.uncommon.enumclasses.PlayStyle;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpellConfiguration {

    // a skill's recovery is what paces it now, so these are the whole pacing vocabulary:
    // a channel beat, the shorter charge/brawler recovery, and the normal 2s one.
    public static final int CHANNEL_CAST_SPEED_TICKS = 10;
    public static final int CHARGE_CAST_SPEED_TICKS = 30;
    public static final int DEFAULT_CAST_SPEED_TICKS = 40;
    public static final int MIN_CAST_SPEED_TICKS = 20;

    // the default gap between two procs of the same skill. a second is short enough to feel responsive
    // and long enough that an on hit proc cannot fire every swing
    public static final int DEFAULT_PROC_COOLDOWN_TICKS = 20;

    // skills used to lean on tiny cooldowns for their feel, so a 1-3 tick cooldown means "as fast as
    // the game allows" rather than a deliberate 0.05s recovery. clamp those up to the 1s floor.
    public static int seedCastSpeedFromCooldown(int cooldownTicks) {
        return Math.max(MIN_CAST_SPEED_TICKS, Math.min(DEFAULT_CAST_SPEED_TICKS, cooldownTicks));
    }

    public boolean swing_arm = true;
    public boolean slows_when_casting = true;
    public boolean channel_skill = false;
    public CastingWeapon castingWeapon = CastingWeapon.ANY_WEAPON;
    public LeveledValue mana_cost = new LeveledValue(0, 0);
    public LeveledValue ene_cost = new LeveledValue(0, 0);
    public int times_to_cast = 1;
    public int charges = 0;
    public int charge_regen = 0;
    public int aggro_radius = 1;
    public int imbues = 0;
    public SummonType summonType = SummonType.NONE;
    public String charge_name = "";
    public String summon_basic_atk = "";

    /**
     * Spell GUIDs a summon spawned by this skill may cast, on top of its {@link #summon_basic_atk}.
     * <p>
     * The list replaces what used to be {@code GolemSummon.aoeSpell()} - a hard-coded abstract method
     * that no datapack could reach and that only golems had at all. Keyed on the summon SKILL rather
     * than on the summoned entity type, so two skills spawning the same mob can hand it different
     * spells, and so a pack that invents a new summon skill gets this for free without a new entity
     * class. Order is irrelevant: the pick is uniformly random, like {@code WizardType.spells}.
     */
    public List<String> summon_spells = new ArrayList<>();

    /**
     * Percent chance, per hit the summon lands, that it casts one of {@link #summon_spells}.
     * <p>
     * 0 means the summon never casts on its own - which is deliberately what the three golems ship
     * with, because their whole cast rate has always come from the owner's Golem Spell Chance stat
     * and moving the spell into the datapack must not quietly hand them a free baseline.
     */
    public int summon_spell_chance = 0;

    /** the shortest gap between two such casts by one summon, so a fast attacker cannot chain them */
    public int summon_spell_cd_ticks = 20;
    private int cast_time_ticks = 0;
    public int cooldown_ticks = 20;
    // the post-cast recovery of this skill, and the global cooldown it puts every other skill on.
    // this is the number the cast speed stats scale, not cooldown_ticks
    public int cast_speed_ticks = DEFAULT_CAST_SPEED_TICKS;
    // how often a proc may trigger this skill. deliberately independent of both numbers above: a proc
    // is not a player cast, so neither Cast Speed nor Cooldown Reduction should change how often it
    // fires. procs used to borrow cooldown_ticks for this, which breaks once skills stop having one.
    // 0 means no limit at all. read straight off config, never through the stat event
    public int proc_cooldown_ticks = DEFAULT_PROC_COOLDOWN_TICKS;
    private String style = PlayStyle.STR.id;
    public TagList<SpellTag> tags = new TagList<>();
    public int tracking_radius = 5;
    public AllyOrEnemy tracks = AllyOrEnemy.enemies;

    public String use_support_gems_from = "";

    public Spell getSpellUsedForSuppGems() {
        return ExileDB.Spells().get(use_support_gems_from);
    }

    public boolean usesSupportGemsFromAnotherSpell() {
        return !use_support_gems_from.isEmpty();
    }

    public SpellConfiguration setTracksNonSelfAllies() {
        this.tracks = AllyOrEnemy.allies_not_self;
        return this;
    }

    public int getCastTimeTicks() {
        return cast_time_ticks;
    }

    public int getCastSpeedTicks() {
        return cast_speed_ticks;
    }

    public SpellConfiguration setCastSpeedTicks(int ticks) {
        this.cast_speed_ticks = ticks;
        return this;
    }

    public SpellConfiguration setProcCooldownTicks(int ticks) {
        this.proc_cooldown_ticks = ticks;
        return this;
    }

    // a channel has no cast time, cast_time_ticks is the gap between pulses instead
    public boolean isChannel() {
        return channel_skill;
    }

    public PlayStyle getStyle() {
        return PlayStyle.fromID(style);
    }


    public SpellConfiguration setStyle(PlayStyle s) {
        this.style = s.id;
        return this;
    }

    public boolean hasSummonBasicAttack() {
        return !summon_basic_atk.isEmpty();
    }

    public Spell getSummonBasicSpell() {
        return ExileDB.Spells().get(summon_basic_atk);
    }

    public boolean hasSummonSpells() {
        return summon_spells != null && !summon_spells.isEmpty();
    }

    /**
     * The extra spells a summon of this skill can actually cast right now.
     * <p>
     * Silently drops ids the spell registry doesn't know, exactly as {@code WizardType.getSpells()}
     * does: a pack can name a skill it never added, or remove one a summon still lists, and the pet
     * should keep fighting with what is left rather than throwing out of the middle of a damage event.
     */
    public List<Spell> getSummonSpells() {
        List<Spell> list = new ArrayList<>();
        if (summon_spells == null) {
            return list;
        }
        for (String id : summon_spells) {
            Spell spell = ExileDB.Spells().get(id);
            if (spell != null) {
                list.add(spell);
            }
        }
        return list;
    }

    public SpellConfiguration setSummonSpells(int chancePercent, String... spellIds) {
        this.summon_spell_chance = chancePercent;
        this.summon_spells = new ArrayList<>(Arrays.asList(spellIds));
        return this;
    }

    public boolean usesCharges() {
        return charges > 0;
    }

    public SpellConfiguration setChargesAndRegen(String name, int charges, int ticksToRegen) {
        this.charge_regen = ticksToRegen;
        this.charges = charges;
        this.charge_name = name;
        // a charge skill is paced by its charges and its recovery, never by a cooldown of its own.
        // SpellBuilder.normalizeCooldown zeroes anything at or below recovery, so this lands at 0 -
        // it stays here so the intent is readable at the call site rather than only at build time
        this.cooldown_ticks = 3;
        this.cast_speed_ticks = CHARGE_CAST_SPEED_TICKS; // charge skills recover faster than everything else
        return this;
    }


    public boolean isProjectile() {
        return tags.contains(SpellTags.projectile);
    }

    public SpellConfiguration setSwingArm() {
        this.swing_arm = true;
        return this;
    }

    public SpellConfiguration setImbue(int times) {
        this.imbues = times;
        return this;
    }

    public SpellConfiguration setTrackingRadius(int rad) {
        this.tracking_radius = rad;
        return this;
    }

    public SpellConfiguration setSummonBasicAttack(String s) {
        this.summon_basic_atk = s;
        return this;
    }

    public SpellConfiguration setUsesSupportGemsFrom(String summonSpellId) {
        this.use_support_gems_from = summonSpellId;
        return this;
    }

    public SpellConfiguration setSummonType(SummonType type) {
        this.summonType = type;
        return this;
    }

    public SpellConfiguration setSummonAggroRadius(int radius) {
        this.aggro_radius = radius;
        return this;
    }


    public static class Builder {
        public static SpellConfiguration energy(int ene, int cd) {
            SpellConfiguration c = new SpellConfiguration();
            c.cast_time_ticks = 0;
            c.ene_cost = new LeveledValue(1F * ene, 0.75F * ene);
            c.cooldown_ticks = cd;
            c.cast_speed_ticks = seedCastSpeedFromCooldown(cd);
            return c;
        }

        public static SpellConfiguration instant(int mana, int cd) {
            SpellConfiguration c = new SpellConfiguration();
            c.cast_time_ticks = 0;
            c.mana_cost = new LeveledValue(1F * mana, 0.75F * mana);
            c.cooldown_ticks = cd;
            c.cast_speed_ticks = seedCastSpeedFromCooldown(cd);
            return c;
        }

        public static SpellConfiguration arrowSpell(int mana, int cd) {
            SpellConfiguration c = new SpellConfiguration();
            c.cast_time_ticks = 0;
            c.mana_cost = new LeveledValue(1F * mana, 0.75F * mana);
            c.cooldown_ticks = cd;
            c.cast_speed_ticks = seedCastSpeedFromCooldown(cd);
            c.swing_arm = false;
            return c;
        }

        public static SpellConfiguration nonInstant(int mana, int cd, int casttime) {
            SpellConfiguration c = new SpellConfiguration();
            c.cast_time_ticks = casttime;
            c.mana_cost = new LeveledValue(1F * mana, 0.75F * mana);
            c.cooldown_ticks = cd;
            c.cast_speed_ticks = seedCastSpeedFromCooldown(cd);
            return c;
        }

        // channels pay per pulse and are held down, so mana is the cost of one pulse and
        // cast_time_ticks is the gap between pulses. cd only applies once the player lets go.
        public static SpellConfiguration channel(int manaPerPulse, int cd, int ticksPerPulse) {
            SpellConfiguration c = new SpellConfiguration();
            c.channel_skill = true;
            c.times_to_cast = 1; // channel replaces the multicast loop, they cannot combine
            c.cast_time_ticks = ticksPerPulse;
            c.mana_cost = new LeveledValue(1F * manaPerPulse, 0.75F * manaPerPulse);
            c.cooldown_ticks = cd;
            // every channel shares one short recovery on release, the beat rate is cast_time_ticks
            c.cast_speed_ticks = CHANNEL_CAST_SPEED_TICKS;
            return c;
        }

        public static SpellConfiguration multiCast(int mana, int cd, int casttime, int times) {
            SpellConfiguration c = new SpellConfiguration();
            c.times_to_cast = times;
            c.cast_time_ticks = casttime;
            c.mana_cost = new LeveledValue(1F * mana, 0.75F * mana);
            c.cooldown_ticks = cd;
            c.cast_speed_ticks = seedCastSpeedFromCooldown(cd);
            return c;
        }

    }
}
