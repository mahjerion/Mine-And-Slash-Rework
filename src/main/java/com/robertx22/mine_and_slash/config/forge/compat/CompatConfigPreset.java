package com.robertx22.mine_and_slash.config.forge.compat;

import com.robertx22.mine_and_slash.database.data.base_stats.BaseStatsConfig;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;

public enum CompatConfigPreset implements IRestrictedConfig<CompatConfigPreset> {
    LITE_MODE(new DefaultCompatData().edit(x -> {
        x.capItemDamage = true;
        x.itemDamageCapNumber = 3;
        x.mobFlatBonusDamage = 0;
        x.mobPercBonusDmg = 0.33F;
        x.statReqMulti = 0.1F;
        x.spellBaseDmgMulti = 2;
        x.balance = GameBalanceConfig.BalanceEnum.COMPAT_BALANCE;
        x.baseStats = BaseStatsConfig.BaseStatsEnum.COMPAT_BALANCE;
        x.enable_newbie_res = false;
        x.disableVanillaHpRegen = false;
        x.energyPenalty = false;
        x.dmgConvert = 30;
        x.dmgConvertLoss = 0;
        x.disableMobIframes = false;
        x.ignoreWepSpellReq = true;
        x.dmgCompat = DamageCompatibilityType.DAMAGE_BONUS;
        x.healthSystem = HealthSystem.VANILLA_HEALTH;

    }), "[Unlocked by installing Mine and Slash Compatibility Addon]: Mine and Slash will be lighted, less invasive and work more like other vanilla-like mods. No more mobs with 1 million hp and so on."),
    ORIGINAL_MODE(new DefaultCompatData().edit(x -> {
        x.dmgConvert = 0;
        x.dmgConvertLoss = 100;
        x.capItemDamage = false;
        x.itemDamageCapNumber = 25;
        x.enable_newbie_res = true;
        x.disableVanillaHpRegen = true;
        x.mobFlatBonusDamage = 6;
        x.mobPercBonusDmg = 0.33F;
        x.spellBaseDmgMulti = 1;
        x.ignoreWepSpellReq = false;
        x.energyPenalty = true;
        x.statReqMulti = 1;
        x.disableMobIframes = true;
        x.baseStats = BaseStatsConfig.BaseStatsEnum.ORIGINAL_BALANCE;
        x.balance = GameBalanceConfig.BalanceEnum.ORIGINAL_BALANCE;
        x.dmgCompat = DamageCompatibilityType.DAMAGE_OVERRIDE;
        x.healthSystem = HealthSystem.IMAGINARY_MINE_AND_SLASH_HEALTH;
    }), "[Default Mine and Slash]: The Original mode, it makes it incompatible with some other mods, like spell mods and overrides damage fully. Makes balancing modpacks a lot easier at the cost of compatibility with some mods."
    ),
    COMPATIBLE_MODE(new DefaultCompatData().edit(x -> {
        x.dmgConvert = 100;
        x.dmgConvertLoss = 100;
        x.capItemDamage = false;
        x.itemDamageCapNumber = 25;
        x.enable_newbie_res = true;
        x.disableVanillaHpRegen = true;
        x.mobFlatBonusDamage = 6;
        x.mobPercBonusDmg = 0.33F;
        x.spellBaseDmgMulti = 1;
        x.ignoreWepSpellReq = false;
        x.energyPenalty = true;
        x.statReqMulti = 1;
        x.disableMobIframes = true;
        x.baseStats = BaseStatsConfig.BaseStatsEnum.ORIGINAL_BALANCE;
        x.balance = GameBalanceConfig.BalanceEnum.ORIGINAL_BALANCE;
        x.dmgCompat = DamageCompatibilityType.DAMAGE_OVERRIDE;
        x.healthSystem = HealthSystem.IMAGINARY_MINE_AND_SLASH_HEALTH;
    }), "[Unlocked by installing Mine and Slash Compatibility Addon]: Similar to the original mode, except spells from other mods will be converted to mine and slash damage and work."
    );


    public DefaultCompatData defaults;

    public String comment;


    CompatConfigPreset(DefaultCompatData defaults, String comment) {
        this.defaults = defaults;
        this.comment = comment;
    }


    @Override
    public CompatConfigPreset getDefaultConfig() {
        return ORIGINAL_MODE;
    }

    @Override
    public boolean isRestricted(CompatConfigPreset obj) {
        return obj != ORIGINAL_MODE;
    }

    @Override
    public CompatConfigPreset getSelf() {
        return this;
    }
}
