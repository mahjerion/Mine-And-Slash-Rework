package com.robertx22.mine_and_slash.config.forge.compat;

import com.robertx22.mine_and_slash.database.data.base_stats.BaseStatsConfig;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public class CompatData implements CompatDummy {
    private ForgeConfigSpec.BooleanValue CAP_ITEM_DAMAGE;
    private ForgeConfigSpec.BooleanValue ENABLE_MINUS_RESISTS_PER_LEVEL;
    private ForgeConfigSpec.BooleanValue DISABLE_VANILLA_HEALTH_REGEN;
    private ForgeConfigSpec.BooleanValue IGNORE_WEAPON_REQUIREMENTS_FOR_SPELLS;
    private ForgeConfigSpec.IntValue ITEM_DAMAGE_CAP_PER_HIT;
    private ForgeConfigSpec.EnumValue<DamageCompatibilityType> DAMAGE_SYSTEM;
    private ForgeConfigSpec.EnumValue<HealthSystem> HEALTH_SYSTEM;
    private ForgeConfigSpec.EnumValue<GameBalanceConfig.BalanceEnum> BALANCE_DATAPACK;
    private ForgeConfigSpec.EnumValue<BaseStatsConfig.BaseStatsEnum> BASE_STATS_DATAPACK;
    private ForgeConfigSpec.DoubleValue MOB_FLAT_DAMAGE_BONUS;
    private ForgeConfigSpec.DoubleValue MOB_PERCENT_DAMAGE_AS_BONUS;
    private ForgeConfigSpec.DoubleValue STAT_REQUIREMENTS_MULTIPLIER;
    private ForgeConfigSpec.DoubleValue SPELL_BASE_DAMAGE_MULTIPLIER;
    private ForgeConfigSpec.DoubleValue VANILLA_TO_WEAPON_DAMAGE_PERCENT;
    private ForgeConfigSpec.BooleanValue ENERGY_PENALTY;
    private ForgeConfigSpec.BooleanValue DISABLE_MOB_IFRAMES;
    private ForgeConfigSpec.IntValue DAMAGE_CONVERSION_LOSS;


    public void build(ForgeConfigSpec.Builder b, DefaultCompatData defaults) {

        DAMAGE_SYSTEM = b.comment("Bonus means mns dmg will act as bonus damage, while override means it will replace the vanilla damage. The Bonus mode requires installing the Compatibility Addon Mod")
                .defineEnum("DAMAGE_SYSTEM", defaults.dmgCompat);

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

        DAMAGE_CONVERSION_LOSS = b
                .comment("This decides how much of the vanilla>mns converted damage is lost.")
                .defineInRange("DAMAGE_CONVERSION_LOSS", defaults.dmgConvertLoss, 0, 100);

        VANILLA_TO_WEAPON_DAMAGE_PERCENT = b
                .comment("When dealing damage with ways Mine and Slash can't detect. For example you cast a spell from another mod, it does 5 vanilla damage, your VANILLA_TO_WEAPON_DAMAGE_PERCENT config is 10, so you end up dealing 50% weapon damage Mine and Slash damage.")
                .defineInRange("VANILLA_TO_WEAPON_DAMAGE_PERCENT", defaults.vanillaToweapondmgPercent, 0, 100);

    }


    @Override
    public boolean capItemDuraLoss() {
        return CAP_ITEM_DAMAGE.get();
    }

    @Override
    public boolean newbieResists() {
        return this.ENABLE_MINUS_RESISTS_PER_LEVEL.get();
    }

    @Override
    public boolean disableVanillaHealthRegen() {
        return DISABLE_VANILLA_HEALTH_REGEN.get();
    }

    @Override
    public boolean ignoreWeaponReqForSpells() {
        return IGNORE_WEAPON_REQUIREMENTS_FOR_SPELLS.get();
    }

    @Override
    public int itemDuraLossCap() {
        return ITEM_DAMAGE_CAP_PER_HIT.get();
    }

    @Override
    public DamageCompatibilityType damageSystem() {
        return DAMAGE_SYSTEM.get();
    }

    @Override
    public HealthSystem healthSystem() {
        return HEALTH_SYSTEM.get();
    }

    @Override
    public GameBalanceConfig.BalanceEnum balanceDatapack() {
        return BALANCE_DATAPACK.get();
    }

    @Override
    public BaseStatsConfig.BaseStatsEnum baseStatsDatapack() {
        return BASE_STATS_DATAPACK.get();
    }

    @Override
    public double mobFlatDmg() {
        return MOB_FLAT_DAMAGE_BONUS.get();
    }

    @Override
    public double mobPercentBonusDamage() {
        return MOB_PERCENT_DAMAGE_AS_BONUS.get();
    }

    @Override
    public double statReqMulti() {
        return STAT_REQUIREMENTS_MULTIPLIER.get();
    }

    @Override
    public double spellBaseDmgMulti() {
        return SPELL_BASE_DAMAGE_MULTIPLIER.get();
    }

    @Override
    public double vanillaToWepDmgPercent() {
        return VANILLA_TO_WEAPON_DAMAGE_PERCENT.get();
    }

    @Override
    public boolean energyPenalty() {
        return ENERGY_PENALTY.get();
    }

    @Override
    public boolean disableMobIframes() {
        return DISABLE_MOB_IFRAMES.get();
    }

    @Override
    public int dmgConversionLoss() {
        return DAMAGE_CONVERSION_LOSS.get();
    }
}
