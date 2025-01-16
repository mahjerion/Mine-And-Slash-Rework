package com.robertx22.mine_and_slash.aoe_data.database.base_stats;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import com.robertx22.mine_and_slash.aoe_data.database.stats.OffenseStats;
import com.robertx22.mine_and_slash.aoe_data.database.stats.ResourceStats;
import com.robertx22.mine_and_slash.aoe_data.database.stats.SpellChangeStats;
import com.robertx22.mine_and_slash.database.data.base_stats.BaseStatsConfig;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.types.offense.WeaponDamage;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.RegeneratePercentStat;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.energy.Energy;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.energy.EnergyRegen;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.health.Health;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.health.HealthRegen;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.magic_shield.MagicShieldRegen;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.mana.Mana;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.mana.ManaRegen;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;

public class BaseStatsAdder implements ExileRegistryInit {

    public static String MOB = "mob";
    public static String EMPTY = "empty";

    @Override
    public void registerAll() {
        playerStatsOverrideMode().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        playerStatsCompatMode().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        mob().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        empty().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
    }

    public static BaseStatsConfig mob() {

        BaseStatsConfig c = new BaseStatsConfig();

        c.id = MOB;

        c.scaled(OffenseStats.ACCURACY.get(), 5);

        return c;

    }

    public static BaseStatsConfig empty() {
        BaseStatsConfig c = new BaseStatsConfig();
        c.id = EMPTY;
        return c;

    }

    public static BaseStatsConfig playerStatsOverrideMode() {

        BaseStatsConfig c = new BaseStatsConfig();

        c.id = BaseStatsConfig.BaseStatsEnum.ORIGINAL_BALANCE.id;

        c.nonScaled(RegeneratePercentStat.MAGIC_SHIELD, 2);

        c.scaled(WeaponDamage.getInstance(), 3);
        c.nonScaled(WeaponDamage.getInstance(), 2);

        c.scaled(Health.getInstance(), 50);
        c.scaled(Mana.getInstance(), 50);
        c.scaled(Energy.getInstance(), 50);

        c.scaled(HealthRegen.getInstance(), 2);
        c.scaled(MagicShieldRegen.getInstance(), 2);
        c.scaled(ManaRegen.getInstance(), 3);
        c.scaled(EnergyRegen.getInstance(), 5);

        c.nonScaled(SpellChangeStats.MAX_SUMMON_CAPACITY.get(), 3);

        // why did i add this again? I think its a must
        c.nonScaled(OffenseStats.CRIT_CHANCE.get(), 1);
        c.nonScaled(OffenseStats.CRIT_DAMAGE.get(), 1);

        for (Stat cap : ResourceStats.LEECH_CAP.getAll()) {
            c.nonScaled(cap, 5);
        }

        return c;

    }


    public static BaseStatsConfig playerStatsCompatMode() {

        BaseStatsConfig c = new BaseStatsConfig();

        c.id = BaseStatsConfig.BaseStatsEnum.COMPAT_BALANCE.id;

        c.nonScaled(RegeneratePercentStat.MAGIC_SHIELD, 2);

        c.scaled(WeaponDamage.getInstance(), 1);

        c.scaled(Health.getInstance(), 10);
        c.scaled(Mana.getInstance(), 50);
        c.scaled(Energy.getInstance(), 50);

        c.scaled(HealthRegen.getInstance(), 0.25F);
        c.scaled(MagicShieldRegen.getInstance(), 1);
        c.scaled(ManaRegen.getInstance(), 1);
        c.scaled(EnergyRegen.getInstance(), 3);

        c.nonScaled(SpellChangeStats.MAX_SUMMON_CAPACITY.get(), 3);

        // why did i add this again? I think its a must
        c.nonScaled(OffenseStats.CRIT_CHANCE.get(), 1);
        c.nonScaled(OffenseStats.CRIT_DAMAGE.get(), 1);

        for (Stat cap : ResourceStats.LEECH_CAP.getAll()) {
            c.nonScaled(cap, 5);
        }

        return c;

    }


}
