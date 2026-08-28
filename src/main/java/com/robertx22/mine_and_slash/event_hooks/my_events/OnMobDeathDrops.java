package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.library_of_exile.components.EntityInfoComponent;
import com.robertx22.library_of_exile.dimension.MapDimensions;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.EntityConfig;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
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

                // a mercenary kill belongs to its owner. resolved from the death itself rather than
                // from killerEntity, because the three steps above almost never produce a mercenary:
                // MercenaryDamageCreditEvent files its damage under the owner, so getHighestDamager
                // returns the owner, not the mercenary that actually swung.
                MercenaryEntity mercKiller = resolveMercKiller(mobKilled, onMobDeath);

                // and the mercenary is still the answer to "who killed this" when nothing else was.
                // must come before the enviro check below: a mercenary's damage also lands in
                // getEnviroOrMobDmg(), so a solo mercenary kill trips that guard and drops out of the
                // whole loot and experience block - awarding nothing, to anyone.
                if (killerEntity == null && mercKiller != null && mercKiller.getOwner() instanceof ServerPlayer mercOwner) {
                    killerEntity = mercOwner;
                }

                if (killerEntity == null) {
                    if (EntityInfoComponent.get(mobKilled)
                            .getDamageStats()
                            .getEnviroOrMobDmg() < mobKilled.getMaxHealth() / 2F) {
                        killerEntity = onMobDeath.killer;
                    }
                }

                if (killerEntity instanceof MercenaryEntity merc && merc.getOwner() instanceof ServerPlayer owner) {
                    mercKiller = merc;
                    killerEntity = owner;
                }

                // in a party, the highest damager can be someone else's character while the finishing
                // blow came from your mercenary. crediting their kill with your mercenary's Bonus
                // Experience and find stats would be wrong, so it only counts for its own owner.
                if (mercKiller != null && mercKiller.getOwner() != killerEntity) {
                    mercKiller = null;
                }

                if (killerEntity instanceof ServerPlayer) {

                    ServerPlayer player = (ServerPlayer) killerEntity;
                    EntityData playerData = Load.Unit(player);

                    EntityConfig config = mobKilledData.getEntityConfig();

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

                        MasterLootGen.genAndDrop(mobKilled, player, mercKiller);

                    }
                    if (exp_multi > 0) {
                        GiveExp(mobKilled, player, playerData, mobKilledData, exp_multi, mercKiller);
                    }


                }

            }

        } catch (
                Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * The mercenary that dealt the killing blow, if there was one.
     * <p>
     * {@code DamageSource.getEntity()} already unwraps a projectile to whoever fired it, so this
     * catches a mercenary's spells as well as its melee. {@code onMobDeath.killer} is the fallback
     * for the paths that never set a last damage source.
     */
    private static MercenaryEntity resolveMercKiller(LivingEntity mobKilled, ExileEvents.OnMobDeath onMobDeath) {
        try {
            if (mobKilled.getLastDamageSource() != null
                    && mobKilled.getLastDamageSource().getEntity() instanceof MercenaryEntity merc) {
                return merc;
            }
        } catch (Exception e) {
            // getLastDamageSource can throw on a partially loaded entity, same as above
        }
        if (onMobDeath.killer instanceof MercenaryEntity merc) {
            return merc;
        }
        return null;
    }

    private static void GiveExp(LivingEntity victim, Player killer, EntityData killerData, EntityData mobData,
                                float multi, MercenaryEntity mercKiller) {

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
        float bonusExp = killerData.getUnit().getCalculatedStat(BonusExp.getInstance()).getMultiplier();

        // a mercenary kill combines its Bonus Experience with its owner's, into the same modifier -
        // it is the same stat, and there is no mercenary-only source of it.
        if (mercKiller != null) {
            bonusExp *= Load.Unit(mercKiller).getUnit().getCalculatedStat(BonusExp.getInstance()).getMultiplier();
        }
        mods.add(new LootModifier(LootModifierEnum.PLAYER_BONUS_EXP, bonusExp));

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

            // mercenaries earn their owner's share, not the party total - hence after the split. they
            // are still not party members for the purposes of that split: TeamUtils only ever returns
            // Players, so list.size() never counts them. and still before the per player level distance
            // penalty applied below, which the design exempts them from.
            giveMercsExp(victim, list, (int) exp);

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

    /**
     * "Only gains XP while alive and actively engaged" - read as alive, spawned, and near enough to
     * the kill to have been part of it. Paid the owner's post-split share, with no level distance
     * penalty (design section 3), and the mercenary never passes its owner's level, which
     * {@code MercenaryManager.giveExp} enforces.
     */
    private static void giveMercsExp(LivingEntity victim, List<Player> team, int exp) {
        if (exp < 1) {
            return;
        }
        double radius = ServerContainer.get().PARTY_RADIUS.get();

        for (Player player : team) {
            if (!Load.player(player).config.isConfigEnabled(PlayerConfigData.Config.ENABLE_EXP_GAIN)) {
                continue;
            }
            MercenaryEntity merc = MercenaryManager.getMerc(player);
            if (merc == null || !merc.isAlive()) {
                continue;
            }
            if (merc.level() != victim.level() || merc.distanceToSqr(victim) > radius * radius) {
                continue;
            }
            MercenaryData data = merc.getMercData();
            if (data != null) {
                MercenaryManager.giveExp(player, data, exp);
            }
        }
    }

}
