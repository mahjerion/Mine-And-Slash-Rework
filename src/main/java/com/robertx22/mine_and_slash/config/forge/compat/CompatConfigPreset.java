package com.robertx22.mine_and_slash.config.forge.compat;

import com.robertx22.mine_and_slash.database.data.base_stats.BaseStatsConfig;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;

public enum CompatConfigPreset {
    FULLY_COMPATIBLE(new DefaultCompatData().edit(x -> {
        x.capItemDamage = true;
        x.itemDamageCapNumber = 3;
        x.mobFlatBonusDamage = 0;
        x.mobPercBonusDmg = 0.33F;
        x.balance = GameBalanceConfig.BalanceEnum.COMPAT_BALANCE;
        x.baseStats = BaseStatsConfig.BaseStatsEnum.COMPAT_BALANCE;
        x.enable_newbie_res = false;
        x.disableVanillaHpRegen = false;
        x.ignoreWepSpellReq = true;
        x.dmgCompat = DamageCompatibilityType.FULL_COMPATIBILITY;

    }), "Makes Mine and Slash play nice with other mods. By default other spell mods will work, mns damage will just be on top of vanilla and other mod damage etc"),
    ORIGINAL_OVERRIDE_MODE(new DefaultCompatData().edit(x -> {
        x.capItemDamage = false;
        x.itemDamageCapNumber = 25;
        x.enable_newbie_res = true;
        x.disableVanillaHpRegen = true;
        x.mobFlatBonusDamage = 6;
        x.mobPercBonusDmg = 0.33F;
        x.ignoreWepSpellReq = false;
        x.baseStats = BaseStatsConfig.BaseStatsEnum.ORIGINAL_BALANCE;
        x.balance = GameBalanceConfig.BalanceEnum.ORIGINAL_BALANCE;
        x.dmgCompat = DamageCompatibilityType.FULL_DAMAGE_OVERRIDE;
    }), "Mine and Slash original mode, it makes it incompatible with some other mods, like spell mods and overrides damage fully. Makes balancing modpacks a lot easier at the cost of compatibility with some mods."
    );

    public DefaultCompatData defaults;

    public String comment;


    CompatConfigPreset(DefaultCompatData defaults, String comment) {
        this.defaults = defaults;
        this.comment = comment;
    }


}
