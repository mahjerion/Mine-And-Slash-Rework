package com.robertx22.mine_and_slash.aoe_data.database.spells;

import com.robertx22.mine_and_slash.database.data.stats.types.resources.energy.Energy;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.health.Health;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.magic_shield.MagicShield;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.mana.Mana;
import com.robertx22.mine_and_slash.database.data.value_calc.ValueCalcBuilder;
import com.robertx22.mine_and_slash.database.data.value_calc.ValueCalculation;
import com.robertx22.mine_and_slash.database.registry.ExileDBInit;

public class SpellCalcs {

    public static void init() {

    }


    public static ValueCalculation EMPTY = ValueCalcBuilder.of(ExileDBInit.UNKNOWN_ID)
            .spellScaling(0, 0)
            .build();


    public static ValueCalculation THORN_CONSUME = ValueCalcBuilder.of("thorn_consume")
            .spellScaling(0.5F, 0.5F)
            .statScaling(Energy.getInstance(), 0.1f, 0.1f)
            .capScaling(1)
            .build();


    public static ValueCalculation PET_BASIC = ValueCalcBuilder.of("pet_basic")
            .spellScaling(1, 1)
            .build();

    public static ValueCalculation SPIDER_PET_BASIC = ValueCalcBuilder.of("spider_pet_basic")
            .spellScaling(0.75F, 0.75F)
            .build();

    public static ValueCalculation EXPLODE_MINION = ValueCalcBuilder.of("explode_minion")
            .spellScaling(0.25F, 1f)
            .targetStatScaling(Health.getInstance(), 0.1F, 0.3F)
            .build();

    public static ValueCalculation CHAOS_TOTEM = ValueCalcBuilder.of("chaos_totem")
            .spellScaling(0.5F, 0.1F)
            .build();


    public static ValueCalculation ARROW_TOTEM = ValueCalcBuilder.of("arrow_totem")
            .spellScaling(0.2F, 0.5F)
            .build();

    // 1.0 rather than the 5 these used to carry. a monster's flat base damage is multiplied by
    // damage effectiveness now (ValueCalculation.mobBaseDamage), and since a monster has no Weapon
    // Damage stat that term IS a boss nova's damage - at 5 it would have quintupled overnight.
    // 1.0 leaves both hitting for what they always did. Raise `base` if bosses want more, not this.
    public static ValueCalculation BOSS_CLOSE_NOVA = ValueCalcBuilder.of("close_nova")
            .spellScaling(1, 1)
            .build();
    public static ValueCalculation BOSS_MINION_EXPLOSION = ValueCalcBuilder.of("minion_explosion")
            .spellScaling(1, 1)
            .build();

    public static ValueCalculation POISON_BALL = ValueCalcBuilder.of("poisonball")
            .spellScaling(0.5F, 0.75F)
            .build();

    public static ValueCalculation ICEBALL = ValueCalcBuilder.of("iceball")
            .spellScaling(0.5F, 0.75F)
            .build();

    public static ValueCalculation HOLY_MISSILES = ValueCalcBuilder.of("holy_missiles")
            .spellScaling(0.15F, 0.25F)
            .build();

    public static ValueCalculation FIREBALL = ValueCalcBuilder.of("fireball")
            .spellScaling(0.5F, 0.75F)
            .build();

    public static ValueCalculation LIGHTNING_SPEAR = ValueCalcBuilder.of("lightning_spear")
            .spellScaling(0.5F, 0.75F)
            .statScaling(Mana.getInstance(), 0.02F, 0.05F)
            .capScaling(1)
            .build();
    public static ValueCalculation BOOMERANG = ValueCalcBuilder.of("boomerang")
            .spellScaling(0.75F, 1.5F)
            .build();

    public static ValueCalculation PETRIFY = ValueCalcBuilder.of("petrify")
            .spellScaling(0.5F, 0.75F)
            .build();

    public static ValueCalculation TORMENT = ValueCalcBuilder.of("torment")
            .attackScaling(0.2F, 0.5F)
            .build();

    public static ValueCalculation DESPAIR = ValueCalcBuilder.of("despair")
            .spellScaling(0.1F, 0.3F)
            .build();
    public static ValueCalculation DIRECT_ARROW_HIT = ValueCalcBuilder.of("direct_arrow_hit")
            .attackScaling(0.5F, 1F)
            .build();

    public static ValueCalculation GONG_STRIKE = ValueCalcBuilder.of("gong_strike")
            .attackScaling(0.1f, 0.3F)
            .statScaling(Health.getInstance(), 0.1F, 0.2F)
            .capScaling(3)
            .build();

    public static ValueCalculation WHIRLWIND = ValueCalcBuilder.of("whirlwind")
            .attackScaling(0.2F, 0.6F)
            .build();

    public static ValueCalculation ARROW_STORM = ValueCalcBuilder.of("arrow_storm")
            .attackScaling(0.3F, 0.6F)
            .build();

    public static ValueCalculation GALE_WIND = ValueCalcBuilder.of("gale_wind")
            .attackScaling(0.5F, 1)
            .build();

    public static ValueCalculation EXPLOSIVE_ARROW = ValueCalcBuilder.of("explosive_arrow")
            .attackScaling(0.5F, 1.5F)
            .build();
    public static ValueCalculation POISON_ARROW = ValueCalcBuilder.of("poison_arrow")
            .attackScaling(0.5F, 1F)
            .build();
    public static ValueCalculation RANGER_TRAP = ValueCalcBuilder.of("ranger_trap")
            .attackScaling(1, 2)
            .build();
    public static ValueCalculation AWAKEN_MANA = ValueCalcBuilder.of("awaken_mana")
            .spellScaling(1, 2)
            .build();
    public static ValueCalculation HUNTER_POTION_HEAL = ValueCalcBuilder.of("hunter_pot_heal")
            .spellScaling(1F, 1F)
            .statScaling(Health.getInstance(), 0.15F, 0.25F)
            .build();
    public static ValueCalculation WISH = ValueCalcBuilder.of("wish")
            .spellScaling(1, 2)
            .build();

    public static ValueCalculation CIRCLE_OF_HEALING = ValueCalcBuilder.of("circle_of_healing")
            .spellScaling(1, 2)
            .statScaling(Energy.getInstance(), 0.05F, 0.05F)
            .capScaling(1)
            .build();

    public static ValueCalculation REJUVENATION = ValueCalcBuilder.of("rejuvenation")
            .spellScaling(0.1F, 0.2F)
            .statScaling(Energy.getInstance(), 0.1F, 0.1F)
            .capScaling(1)
            .build();

    public static ValueCalculation INNER_CALM = ValueCalcBuilder.of("inner_calm")
            .spellScaling(0.05F, 0.1F)
            .statScaling(Energy.getInstance(), 0.1F, 0.2F)
            .capScaling(2)
            .build();

    public static ValueCalculation POWER_CHORD = ValueCalcBuilder.of("power_chord")
            .spellScaling(0.5F, 1F)
            .build();
    public static ValueCalculation RESONANCE = ValueCalcBuilder.of("resonance")
            .spellScaling(0.2F, 0.4F)
            .build();
    public static ValueCalculation RITARDANDO = ValueCalcBuilder.of("ritardando")
            .spellScaling(1.0F, 2F)
            .build();
    public static ValueCalculation SHOOTING_STAR = ValueCalcBuilder.of("shooting_star")
            .spellScaling(0.5F, 1.5F)
            .build();
    public static ValueCalculation TIDAL_STRIKE = ValueCalcBuilder.of("tidal_strike")
            .attackScaling(0.4F, 0.75F)
            .build();
    public static ValueCalculation LIGHTNING_TOTEM = ValueCalcBuilder.of("lightning_totem")
            .attackScaling(0.5F, 1)
            .build();
    public static ValueCalculation FROST_FLOWER = ValueCalcBuilder.of("flower_flower")
            .attackScaling(0.5F, 1)
            .build();
    public static ValueCalculation FIRE_NOVA = ValueCalcBuilder.of("fire_nova")
            .spellScaling(1F, 2)
            .build();
    public static ValueCalculation METEOR = ValueCalcBuilder.of("meteor")
            .spellScaling(1F, 2F)
            .build();

    public static ValueCalculation ICE_COMET = ValueCalcBuilder.of("ice_comet")
            .spellScaling(0.75F, 1.5F)
            .build();

    public static ValueCalculation BLIZZARD = ValueCalcBuilder.of("blizzard")
            .spellScaling(0.2F, 0.4F)
            .statScaling(MagicShield.getInstance(), 0.1F, 0.2F)
            .capScaling(3)
            .build();


    public static ValueCalculation SHATTER_PROC = ValueCalcBuilder.of("shatter")
            .spellScaling(0.25F, 0.5F)
            .build();

    public static ValueCalculation PROFANE_EXPLOSION = ValueCalcBuilder.of("profane_explosion")
            .spellScaling(1, 1)
            .build();
    public static ValueCalculation BLOOD_EXPLOSION = ValueCalcBuilder.of("blood_explosion")
            .spellScaling(1, 1)
            .build();
    public static ValueCalculation IGNITE_EXPLOSION = ValueCalcBuilder.of("ignite_explosion")
            .spellScaling(2, 2)
            .build();

    public static ValueCalculation FLAME_STRIKE = ValueCalcBuilder.of("flame_strike")
            .attackScaling(0.4F, 0.75F)
            .build();
    public static ValueCalculation CHILLING_TOUCH = ValueCalcBuilder.of("chilling_touch")
            .attackScaling(0.5F, 0.75F)
            .build();

    public static ValueCalculation HEALING_AURA = ValueCalcBuilder.of("healing_aura")
            .spellScaling(0.3F, 0.6F)
            .build();
    public static ValueCalculation HEART_OF_ICE = ValueCalcBuilder.of("heart_of_ice")
            .spellScaling(0.5F, 1F)
            .statScaling(MagicShield.getInstance(), 0.1F, 0.2F)
            .build();
    public static ValueCalculation FROST_NOVA = ValueCalcBuilder.of("frost_nova")
            .spellScaling(1F, 2)
            .build();

    public static ValueCalculation LIGHNING_NOVA = ValueCalcBuilder.of("lightning_nova")
            .spellScaling(1F, 1.5F)
            .statScaling(Mana.getInstance(), 0.02F, 0.05F)
            .capScaling(1)
            .build();

    public static ValueCalculation POISON_CLOUD = ValueCalcBuilder.of("poison_cloud")
            .spellScaling(1, 2)
            .build();

    public static ValueCalculation SHOUT_WARN = ValueCalcBuilder.of("shout_warn")
            .statScaling(Health.getInstance(), 0.05F, 0.1F)
            .build();
    public static ValueCalculation CHARGED_BOLT = ValueCalcBuilder.of("charged_bolt")
            .attackScaling(0.5F, 1F)
            .build();
    public static ValueCalculation EXECUTE = ValueCalcBuilder.of("execute")
            .attackScaling(1F, 2F)
            .build();
    public static ValueCalculation CHARGE = ValueCalcBuilder.of("charge")
            .attackScaling(0.3F, 0.6F)
            .build();
    public static ValueCalculation TAUNT = ValueCalcBuilder.of("taunt")
            .statScaling(Health.getInstance(), 0.05F, 0.1F)
            .build();
    public static ValueCalculation PULL = ValueCalcBuilder.of("pull")
            .attackScaling(0.2F, 0.3F)
            .build();
    public static ValueCalculation SHRED = ValueCalcBuilder.of("shred")
            .attackScaling(0.3F, 0.6F)
            .build();
    public static ValueCalculation TOTEM_HEAL = ValueCalcBuilder.of("totem_heal")
            .spellScaling(0.25F, 0.6F)
            .build();
    public static ValueCalculation TOTEM_GUARD = ValueCalcBuilder.of("totem_guard")
            .spellScaling(0.2F, 0.5F)
            .build();
    public static ValueCalculation TOTEM_MANA = ValueCalcBuilder.of("totem_mana")
            .spellScaling(0.25F, 0.6F)
            .build();

    public static ValueCalculation CURSE = ValueCalcBuilder.of("curse")
            .spellScaling(0.5F, 1F)
            .build();

    public static ValueCalculation BLACK_HOLE = ValueCalcBuilder.of("black_hole")
            .spellScaling(0.5F, 1.5F)
            .build();

    public static ValueCalculation THORN_BUSH = ValueCalcBuilder.of("thorn_bush")
            .spellScaling(0.1F, 0.25F)
            .build();

    public static ValueCalculation MAGMA_FLOWER = ValueCalcBuilder.of("magma_flower")
            .spellScaling(0.5F, 1F)
            .build();
    public static ValueCalculation CHILLING_FIELD = ValueCalcBuilder.of("chilling_field")
            .spellScaling(0.2F, 0.5F)
            .build();
    public static ValueCalculation SMOKE_BOMB = ValueCalcBuilder.of("lose_aggro")
            .spellScaling(2, 4)
            .build();

    // --------------------------------------------------------------------- wizard mobs
    //
    // One calc per witch_* skill, all of them flatWeaponScaling: base zero, everything pinned
    // min == max. The wizards are monsters, so the Weapon Damage scaling multiplies a stat they
    // don't have and the number below is really the damage effectiveness - which is what scales
    // their flat mob damage, in ValueCalculation.mobBaseDamage.
    //
    // Sized by HITS PER CAST, not by the player skill they were copied from, because that term is
    // collected once per damage activation. The budget is WIZARD_HIT_BUDGET of it per cast, split
    // by however many times the skill lands - so a single fireball and a field that pulses six
    // times deal the same total, and the choice between them is about area and dodging rather than
    // about which one happens to tick more. Tune the budget to move every wizard at once, or one
    // divisor to move one skill.

    /** total damage effectiveness one wizard cast is worth, however many hits it lands */
    private static final float WIZARD_HIT_BUDGET = 2F;

    private static ValueCalculation witch(String id, float hitsPerCast) {
        return ValueCalcBuilder.of(id).flatWeaponScaling(WIZARD_HIT_BUDGET / hitsPerCast, 0).build();
    }

    public static ValueCalculation WITCH_FIREBALL = witch("witch_fireball", 1);
    public static ValueCalculation WITCH_FIRE_NOVA = witch("witch_fire_nova", 1);
    public static ValueCalculation WITCH_METEOR = witch("witch_meteor", 1);

    public static ValueCalculation WITCH_FROSTBALL = witch("witch_frostball", 1);
    // the orb drifts for three seconds pulsing once a second, and detonates at the end
    public static ValueCalculation WITCH_FROZEN_ORB = witch("witch_frozen_orb", 4);
    // an air block that lives six seconds and ticks once a second
    public static ValueCalculation WITCH_CHILLING_FIELD = witch("witch_chilling_field", 6);

    public static ValueCalculation WITCH_LIGHTNING_SPEAR = witch("witch_lightning_spear", 1);
    // three chains, so three things hit - or one thing hit once, when a player is alone
    public static ValueCalculation WITCH_CHAIN_LIGHTNING = witch("witch_chain_lightning", 3);
    // same six seconds at once a second as the field
    public static ValueCalculation WITCH_LIGHTNING_TOTEM = witch("witch_lightning_totem", 6);

    public static ValueCalculation WITCH_POISON_BALL = witch("witch_poison_ball", 1);

}
