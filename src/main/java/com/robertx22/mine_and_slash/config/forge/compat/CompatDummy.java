package com.robertx22.mine_and_slash.config.forge.compat;

import com.robertx22.mine_and_slash.database.data.base_stats.BaseStatsConfig;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;

public interface CompatDummy {

    boolean capItemDuraLoss();

    boolean newbieResists();

    boolean disableVanillaHealthRegen();

    boolean ignoreWeaponReqForSpells();

    int itemDuraLossCap();

    DamageCompatibilityType damageSystem();

    HealthSystem healthSystem();

    GameBalanceConfig.BalanceEnum balanceDatapack();

    BaseStatsConfig.BaseStatsEnum baseStatsDatapack();

    double mobFlatDmg();

    double mobPercentBonusDamage();

    double statReqMulti();

    double spellBaseDmgMulti();

    double vanillaToWepDmgPercent();

    boolean energyPenalty();

    boolean disableMobIframes();

    int dmgConversionLoss();
}


