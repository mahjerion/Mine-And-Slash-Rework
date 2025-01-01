package com.robertx22.mine_and_slash.config.forge.compat;

import com.robertx22.mine_and_slash.database.data.base_stats.BaseStatsConfig;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public class CompatData {
    public ForgeConfigSpec.BooleanValue CAP_ITEM_DAMAGE;
    public ForgeConfigSpec.BooleanValue ENABLE_MINUS_RESISTS_PER_LEVEL;
    public ForgeConfigSpec.BooleanValue DISABLE_VANILLA_HEALTH_REGEN;
    public ForgeConfigSpec.BooleanValue IGNORE_WEAPON_REQUIREMENTS_FOR_SPELLS;
    public ForgeConfigSpec.IntValue ITEM_DAMAGE_CAP_PER_HIT;
    public ForgeConfigSpec.EnumValue<DamageCompatibilityType> DAMAGE_COMPATIBILITY;
    public ForgeConfigSpec.EnumValue<HealthSystem> HEALTH_SYSTEM;
    public ForgeConfigSpec.EnumValue<GameBalanceConfig.BalanceEnum> BALANCE_DATAPACK;
    public ForgeConfigSpec.EnumValue<BaseStatsConfig.BaseStatsEnum> BASE_STATS_DATAPACK;
    public ForgeConfigSpec.DoubleValue MOB_FLAT_DAMAGE_BONUS;
    public ForgeConfigSpec.DoubleValue MOB_PERCENT_DAMAGE_AS_BONUS;
    public ForgeConfigSpec.DoubleValue STAT_REQUIREMENTS_MULTIPLIER;
    public ForgeConfigSpec.DoubleValue SPELL_BASE_DAMAGE_MULTIPLIER;
    public ForgeConfigSpec.BooleanValue ENERGY_PENALTY;
    public ForgeConfigSpec.BooleanValue DISABLE_MOB_IFRAMES;


    public void build(ForgeConfigSpec.Builder b, DefaultCompatData defaults) {


        DAMAGE_COMPATIBILITY = b.comment("Compat means mns dmg will act as bonus damage, while override means it will replace the vanilla damage.")
                .defineEnum("DAMAGE_COMPATIBILITY", defaults.dmgCompat);

        HEALTH_SYSTEM = b.comment("Vanilla means mns will add to your hearts, imaginary means mns won't add hearts but instead just scale damage based on mob's imaginary/mns hp")
                .defineEnum("HEALTH_SYSTEM", defaults.healthSystem);

        DISABLE_VANILLA_HEALTH_REGEN = b
                .define("DISABLE_VANILLA_HEALTH_REGEN", defaults.disableVanillaHpRegen);

        ENERGY_PENALTY = b.comment("When trying to attack on low energy, you will get slow and hunger.")
                .define("ENERGY_PENALTY", defaults.energyPenalty);

        IGNORE_WEAPON_REQUIREMENTS_FOR_SPELLS = b
                .define("IGNORE_WEAPON_REQUIREMENTS_FOR_SPELLS", defaults.ignoreWepSpellReq);

        BALANCE_DATAPACK = b.comment("The balance datapack the game will use. Compat mode for example has lower stat scalings by default.")
                .defineEnum("BALANCE_DATAPACK", defaults.balance);

        BASE_STATS_DATAPACK = b.comment("The player base stats datapack the game will use. Compat mode gives player less base stats for example.")
                .defineEnum("BASE_STATS_DATAPACK", defaults.baseStats);

        ITEM_DAMAGE_CAP_PER_HIT = b
                .comment("Caps how much your items can be damaged in a single hit. This prevents items insta-breaking when you have high amounts of HP")
                .defineInRange("ITEM_DAMAGE_CAP_PER_HIT", defaults.itemDamageCapNumber, 0, 100);

        CAP_ITEM_DAMAGE = b
                .comment("Enables item damage cap. WARNING! Disabling this means your items will instantly break if you get hit by a high level mob")
                .define("CAP_ITEM_DAMAGE", defaults.capItemDamage);

        ENABLE_MINUS_RESISTS_PER_LEVEL = b
                .comment("By default you have a lot of free newbie resistances at the start but they go in minus upon reaching certain levels. Best disabled if playing without high level scaling.")
                .define("ENABLE_MINUS_RESISTS_PER_LEVEL", defaults.enable_newbie_res);

        MOB_FLAT_DAMAGE_BONUS = b.defineInRange("MOB_FLAT_DAMAGE_BONUS", defaults.mobFlatBonusDamage, 0, 100);
        MOB_PERCENT_DAMAGE_AS_BONUS = b.defineInRange("MOB_PERCENT_DAMAGE_AS_BONUS", defaults.mobPercBonusDmg, 0, 100);
        STAT_REQUIREMENTS_MULTIPLIER = b.defineInRange("STAT_REQUIREMENTS_MULTIPLIER", defaults.statReqMulti, 0, 100);
        SPELL_BASE_DAMAGE_MULTIPLIER = b.defineInRange("SPELL_BASE_DAMAGE_MULTIPLIER", defaults.spellBaseDmgMulti, 0, 100);
        DISABLE_MOB_IFRAMES = b.define("DISABLE_MOB_IFRAMES", defaults.disableMobIframes);


    }
}
