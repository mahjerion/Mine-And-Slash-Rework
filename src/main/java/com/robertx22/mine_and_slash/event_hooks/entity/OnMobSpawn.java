package com.robertx22.mine_and_slash.event_hooks.entity;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.EntityConfig;
import com.robertx22.mine_and_slash.database.data.rarities.MobRarity;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.EpicMonsterChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.LegendaryMonsterChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.MobModifierDensity;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.MythicMonsterChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.RareMonsterChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.UncommonMonsterChance;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.unit.Unit;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class OnMobSpawn {

    public static void onLoad(Entity entity) {

        if (entity == null) {
            return;
        }

        if (!(entity instanceof LivingEntity)) {
            return;
        }
        if (entity instanceof Player) {
            return;
        }
        Load.Unit(entity).immuneTicks = 10;

        setupNewMobOnSpawn((LivingEntity) entity);


    }

    public static void setupNewMobOnSpawn(LivingEntity entity) {

        if (entity.level().isClientSide) {
            return;
        }

        EntityData endata = Load.Unit(entity);

        if (endata != null) {

            endata.setType();

            Player nearestPlayer = null;

            nearestPlayer = PlayerUtils.nearestPlayer((ServerLevel) entity.level(), entity);

            if (endata.needsToBeGivenStats()) {
                setupNewMob(entity, endata, nearestPlayer);
                //entity.heal(Integer.MAX_VALUE);
            } else {
                if (endata.getUnit() == null) {
                    endata.setUnit(new Unit());
                }
                endata.getUnit().initStats(); // give new stats to mob on spawn
                endata.setEquipsChanged();
            }
            endata.sync.setDirty();
        }

    }

    public static Unit setupNewMob(LivingEntity entity, EntityData endata, Player nearestPlayer) {
        EntityConfig config = endata.getEntityConfig();

        Unit mob = new Unit();
        mob.initStats();

        endata.SetMobLevelAtSpawn(nearestPlayer);

        String rar = endata.getRarity();

        float densityBonus = 0;
        Map<String, Float> perRarityBonus = new HashMap<>();
        if (nearestPlayer != null) {
            Unit playerUnit = Load.Unit(nearestPlayer).getUnit();
            densityBonus = playerUnit.getCalculatedStat(MobModifierDensity.getInstance()).getValue();
            perRarityBonus.put(IRarity.UNCOMMON, playerUnit.getCalculatedStat(UncommonMonsterChance.getInstance()).getValue());
            perRarityBonus.put(IRarity.RARE_ID, playerUnit.getCalculatedStat(RareMonsterChance.getInstance()).getValue());
            perRarityBonus.put(IRarity.EPIC_ID, playerUnit.getCalculatedStat(EpicMonsterChance.getInstance()).getValue());
            perRarityBonus.put(IRarity.LEGENDARY_ID, playerUnit.getCalculatedStat(LegendaryMonsterChance.getInstance()).getValue());
            perRarityBonus.put(IRarity.MYTHIC_ID, playerUnit.getCalculatedStat(MythicMonsterChance.getInstance()).getValue());
        }
        rar = mob.randomRarity(endata.getLevel(), endata, densityBonus, perRarityBonus);

        if (config.hasSpecificRarity()) {
            rar = config.set_rar;
        }


        endata.setRarity(rar);

        MobRarity rarity = ExileDB.MobRarities().get(rar);
        endata.getAffixData().randomizeAffixes(rarity);

        endata.setUnit(mob);

        endata.mobStatsAreSet();
        endata.setEquipsChanged();

        return mob;

    }

}
