package com.robertx22.mine_and_slash.aoe_data.database.base_stats;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SummonType;
import com.robertx22.mine_and_slash.aoe_data.database.stats.DefenseStats;
import com.robertx22.mine_and_slash.aoe_data.database.stats.OffenseStats;
import com.robertx22.mine_and_slash.aoe_data.database.stats.ResourceStats;
import com.robertx22.mine_and_slash.aoe_data.database.stats.SpellChangeStats;
import com.robertx22.mine_and_slash.database.data.base_stats.BaseStatsConfig;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.BlockDamageReduction;
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
    /** the mercenary companion kit - tuned apart from both the player and the mob one */
    public static String MERCENARY = "mercenary";

    @Override
    public void registerAll() {
        playerStatsOverrideMode().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        playerStatsCompatMode().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        mob().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        mercenary().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        empty().addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
    }

    public static BaseStatsConfig mob() {

        BaseStatsConfig c = new BaseStatsConfig();

        c.id = MOB;

        c.scaled(OffenseStats.ACCURACY.get(), 5);

        // 0 on purpose. the stat's own base of 100 only reaches a unit's calculated stats once
        // something puts it in the calculation at all
        c.nonScaled(BlockDamageReduction.getInstance(), 0);

        return c;

    }

    /**
     * The mercenary starter kit. Deliberately a full copy of {@link #playerStatsOverrideMode()}
     * rather than a call to it - a mercenary is meant to be balanced apart from the character, and
     * sharing the body would make the first tweak to either one silently move the other.
     */
    public static BaseStatsConfig mercenary() {

        BaseStatsConfig c = new BaseStatsConfig();

        c.id = MERCENARY;

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

        for (SummonType t : SummonType.getCapped()) {
            c.nonScaled(SpellChangeStats.MAX_SUMMONS_PER_TYPE.get(t), t.maxSummons);
        }
        c.nonScaled(SpellChangeStats.MAX_TOTEM_CAPACITY.get(), 3);
        c.nonScaled(SpellChangeStats.MAX_BANNER_CAPACITY.get(), 1);

        c.nonScaled(DefenseStats.NO_SELF_DAMAGE_STATS.get(), 1);

        c.nonScaled(OffenseStats.CRIT_CHANCE.get(), 1);
        c.nonScaled(OffenseStats.CRIT_DAMAGE.get(), 1);

        for (Stat cap : ResourceStats.LEECH_CAP.getAll()) {
            c.nonScaled(cap, 5);
        }

        // 0 on purpose. the stat's own base of 100 only reaches the character screen once something
        // puts it in the calculation at all, the same reason crit damage is listed above
        c.nonScaled(BlockDamageReduction.getInstance(), 0);

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

        for (SummonType t : SummonType.getCapped()) {
            c.nonScaled(SpellChangeStats.MAX_SUMMONS_PER_TYPE.get(t), t.maxSummons);
        }
        c.nonScaled(SpellChangeStats.MAX_TOTEM_CAPACITY.get(), 3);
        c.nonScaled(SpellChangeStats.MAX_BANNER_CAPACITY.get(), 1);

        c.nonScaled(DefenseStats.NO_SELF_DAMAGE_STATS.get(), 1);

        // why did i add this again? I think its a must
        c.nonScaled(OffenseStats.CRIT_CHANCE.get(), 1);
        c.nonScaled(OffenseStats.CRIT_DAMAGE.get(), 1);

        for (Stat cap : ResourceStats.LEECH_CAP.getAll()) {
            c.nonScaled(cap, 5);
        }

        // 0 on purpose. the stat's own base of 100 only reaches the character screen once something
        // puts it in the calculation at all, the same reason crit damage is listed above
        c.nonScaled(BlockDamageReduction.getInstance(), 0);

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
        
        c.nonScaled(DefenseStats.NO_SELF_DAMAGE_STATS.get(), 1);

        for (SummonType t : SummonType.getCapped()) {
            c.nonScaled(SpellChangeStats.MAX_SUMMONS_PER_TYPE.get(t), t.maxSummons);
        }
        c.nonScaled(SpellChangeStats.MAX_TOTEM_CAPACITY.get(), 3);
        c.nonScaled(SpellChangeStats.MAX_BANNER_CAPACITY.get(), 1);

        // why did i add this again? I think its a must
        c.nonScaled(OffenseStats.CRIT_CHANCE.get(), 1);
        c.nonScaled(OffenseStats.CRIT_DAMAGE.get(), 1);

        for (Stat cap : ResourceStats.LEECH_CAP.getAll()) {
            c.nonScaled(cap, 5);
        }

        // 0 on purpose. the stat's own base of 100 only reaches the character screen once something
        // puts it in the calculation at all, the same reason crit damage is listed above
        c.nonScaled(BlockDamageReduction.getInstance(), 0);

        return c;

    }


}
