package com.robertx22.mine_and_slash.config.forge.compat;

import com.robertx22.mine_and_slash.database.data.base_stats.BaseStatsConfig;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;

import java.util.function.Consumer;

public class DefaultCompatData implements CompatDummy {


    static {


    }

    public DefaultCompatData edit(Consumer<DefaultCompatData> c) {
        c.accept(this);
        return this;
    }

    public DamageCompatibilityType dmgCompat = DamageCompatibilityType.DAMAGE_BONUS;
    public HealthSystem healthSystem = HealthSystem.IMAGINARY_MINE_AND_SLASH_HEALTH;
    public boolean capItemDamage = true;
    public boolean disableVanillaHpRegen = true;
    public boolean ignoreWepSpellReq = true;
    public boolean energyPenalty = true;
    public boolean disableMobIframes = true;
    public int itemDamageCapNumber = 3;
    public int dmgConvertLoss = 100;
    public double mobFlatBonusDamage = 6;
    public double mobPercBonusDmg = 0.33F;
    public double statReqMulti = 1;
    public double spellBaseDmgMulti = 1;
    public double vanillaToweapondmgPercent = 10;
    public boolean enable_newbie_res = false;
    public GameBalanceConfig.BalanceEnum balance = GameBalanceConfig.BalanceEnum.COMPAT_BALANCE;
    public BaseStatsConfig.BaseStatsEnum baseStats = BaseStatsConfig.BaseStatsEnum.COMPAT_BALANCE;


    @Override
    public boolean capItemDuraLoss() {
        return capItemDamage;
    }

    @Override
    public boolean newbieResists() {
        return enable_newbie_res;
    }

    @Override
    public boolean disableVanillaHealthRegen() {
        return disableVanillaHpRegen;
    }

    @Override
    public boolean ignoreWeaponReqForSpells() {
        return ignoreWepSpellReq;
    }

    @Override
    public int itemDuraLossCap() {
        return itemDamageCapNumber;
    }

    @Override
    public DamageCompatibilityType damageSystem() {
        return this.dmgCompat;
    }

    @Override
    public HealthSystem healthSystem() {
        return healthSystem;
    }

    @Override
    public GameBalanceConfig.BalanceEnum balanceDatapack() {
        return balance;
    }

    @Override
    public BaseStatsConfig.BaseStatsEnum baseStatsDatapack() {
        return baseStats;
    }

    @Override
    public double mobFlatDmg() {
        return mobFlatBonusDamage;
    }

    @Override
    public double mobPercentBonusDamage() {
        return mobPercBonusDmg;
    }

    @Override
    public double statReqMulti() {
        return statReqMulti;
    }

    @Override
    public double spellBaseDmgMulti() {
        return spellBaseDmgMulti;
    }

    @Override
    public double vanillaToWepDmgPercent() {
        return vanillaToweapondmgPercent;
    }

    @Override
    public boolean energyPenalty() {
        return energyPenalty;
    }

    @Override
    public boolean disableMobIframes() {
        return disableMobIframes;
    }

    @Override
    public int dmgConversionLoss() {

        return dmgConvertLoss;
    }
}
