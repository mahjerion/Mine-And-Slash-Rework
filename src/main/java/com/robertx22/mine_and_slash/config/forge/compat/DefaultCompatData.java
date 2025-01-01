package com.robertx22.mine_and_slash.config.forge.compat;

import com.robertx22.mine_and_slash.database.data.base_stats.BaseStatsConfig;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;

import java.util.function.Consumer;

public class DefaultCompatData {


    static {


    }

    public DefaultCompatData edit(Consumer<DefaultCompatData> c) {
        c.accept(this);
        return this;
    }

    public DamageCompatibilityType dmgCompat = DamageCompatibilityType.FULL_COMPATIBILITY;
    public HealthSystem healthSystem = HealthSystem.IMAGINARY_MINE_AND_SLASH_HEALTH;
    public boolean capItemDamage = true;
    public boolean disableVanillaHpRegen = true;
    public boolean ignoreWepSpellReq = true;
    public boolean energyPenalty = true;
    public boolean disableMobIframes = true;
    public int itemDamageCapNumber = 3;
    public double mobFlatBonusDamage = 6;
    public double mobPercBonusDmg = 0.33F;
    public double statReqMulti = 1;
    public double spellBaseDmgMulti = 1;
    public boolean enable_newbie_res = false;
    public GameBalanceConfig.BalanceEnum balance = GameBalanceConfig.BalanceEnum.COMPAT_BALANCE;
    public BaseStatsConfig.BaseStatsEnum baseStats = BaseStatsConfig.BaseStatsEnum.COMPAT_BALANCE;

}
