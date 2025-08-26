package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.library_of_exile.components.EntityInfoComponent;
import com.robertx22.library_of_exile.dimension.MapDimensions;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.EntityConfig;
import com.robertx22.mine_and_slash.database.data.stats.types.misc.BonusExp;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.*;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.LevelUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.TeamUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class OnMobDeathDrops extends EventConsumer<ExileEvents.OnMobDeath> {

    @Override
    public void accept(ExileEvents.OnMobDeath onMobDeath) {
        LivingEntity mobKilled = onMobDeath.mob;

        try {

            if (mobKilled.level().isClientSide) {
                return;
            }


            if (!(mobKilled instanceof Player)) {


                EntityData mobKilledData = Load.Unit(mobKilled);


                // todo doesnt work
                LivingEntity killerEntity = EntityInfoComponent.get(mobKilled)
                        .getDamageStats()
                        .getHighestDamager((ServerLevel) mobKilled.level());

                if (killerEntity == null) {
                    try {
                        if (mobKilled.getLastDamageSource()
                                .getEntity() instanceof Player) {
                            killerEntity = (LivingEntity) mobKilled.getLastDamageSource()
                                    .getEntity();
                        }
                    } catch (Exception e) {
                    }
                }

                if (killerEntity == null) {
                    if (EntityInfoComponent.get(mobKilled)
                            .getDamageStats()
                            .getEnviroOrMobDmg() < mobKilled.getMaxHealth() / 2F) {
                        killerEntity = onMobDeath.killer;
                    }
                }

                if (killerEntity instanceof ServerPlayer) {

                    ServerPlayer player = (ServerPlayer) killerEntity;
                    EntityData playerData = Load.Unit(player);

                    EntityConfig config = ExileDB.getEntityConfig(mobKilled, mobKilledData);

                    float loot_multi = (float) config.loot_multi;
                    float exp_multi = (float) config.exp_multi;


                    if (loot_multi > 0) {


                        var map = MapDimensions.getInfo(mobKilled.level());

                        if (map != null) {
                            if (!map.mobValidator.isValidMob(mobKilled)) {
                                if (MMORPG.RUN_DEV_TOOLS) {
                                    player.sendSystemMessage(Component.literal("Killed Mob wasn't properly spawned"));
                                }
                                return;
                            }
                        }

                        MasterLootGen.genAndDrop(mobKilled, player);

                    }
                    if (exp_multi > 0) {
                        GiveExp(mobKilled, player, playerData, mobKilledData, exp_multi);
                    }


                }

            }

        } catch (
                Exception e) {
            e.printStackTrace();
        }

    }


    private static void GiveExp(LivingEntity victim, Player killer, EntityData killerData, EntityData mobData,
                                float multi) {

        float exp = LevelUtils.getBaseExpMobReward(mobData.getLevel());

        if (exp < 1) {
            exp++;
        }

        LootModifiersList mods = new LootModifiersList();

        mods.add(new LootModifier(LootModifierEnum.MOB_HEALTH, LootUtils.getMobHealthBasedLootMulti(victim)));
        mods.add(new LootModifier(LootModifierEnum.MOB_RARITY, mobData.getMobRarity().expMulti()));
        mods.add(new LootModifier(LootModifierEnum.MOB_DATAPACK, multi));
        mods.add(new LootModifier(LootModifierEnum.EXP_GAIN_CONFIG, ServerContainer.get().EXP_GAIN_MULTI.get().floatValue()));
        mods.add(new LootModifier(LootModifierEnum.DIMENSION_LOOT, ExileDB.getDimensionConfig(victim.level()).exp_multi));
        mods.add(new LootModifier(LootModifierEnum.PLAYER_BONUS_EXP, killerData.getUnit().getCalculatedStat(BonusExp.getInstance()).getMultiplier()));
        mods.add(new LootModifier(LootModifierEnum.FAVOR, Load.player(killer).favor.getLootExpMulti()));

        WorldUtils.ifMapData(victim.level(), victim.blockPosition()).ifPresent(map -> {
            mods.add(new LootModifier(LootModifierEnum.ADVENTURE_MAP, map.map.getExpMulti()));
        });

        for (LootModifier mod : mods.all) {
            exp *= mod.multi;
        }

        // todo rework this into multi
        exp = ExileEvents.MOB_EXP_DROP.callEvents(new ExileEvents.OnMobExpDrop(victim, exp)).exp;


        if ((int) exp > 0) {

            List<Player> list = TeamUtils.getOnlineTeamMembersInRange(killer);

            int members = list.size() - 1;
            if (members > 4) {
                members = 4;
            }

            float teamMulti = (float) (1 + (ServerContainer.get().PARTY_EXP_BONUS.get() * members));

            exp *= teamMulti;

            exp /= list.size();

            for (Player player : list) {
                var canReceiveExp = Load.player(player).config.isConfigEnabled(PlayerConfigData.Config.ENABLE_EXP_GAIN);
                if (!canReceiveExp) {
                    continue;
                }

                int splitExp = (int) (exp * LootUtils.getLevelDistancePunishmentMulti(mobData.getLevel(), Load.Unit(player).getLevel()));

                if (splitExp > 0) {
                    Load.Unit(player).GiveExp(player, splitExp, mods);
                }
            }

        }
    }

}
