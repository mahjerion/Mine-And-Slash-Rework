package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.DimensionConfig;
import com.robertx22.mine_and_slash.database.data.MinMax;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.database.data.level_ranges.LevelRange;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.levels.LevelInfo;
import com.robertx22.temp.SkillItemTier;
import com.robertx22.the_harvest.main.HarvestMain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public class LevelUtils {


    public static int getMaxTier() {
        return levelToTier(GameBalanceConfig.get().MAX_LEVEL);
    }


    public static void runTests() {
        if (MMORPG.RUN_DEV_TOOLS) {
            // Preconditions.checkArgument(levelToTier(15) == 0);
            //Preconditions.checkArgument(levelToTier(25) == 1);
        }
    }


    public static LevelRange tierToLevel(int tier) {
        return SkillItemTier.of(tier).levelRange;
    }

    public static int levelToTier(int level) {
        return SkillItemTier.fromLevel(level).tier;
    }


    public static float getMaxLevelMultiplier(float lvl) {
        float max = GameBalanceConfig.get().MAX_LEVEL;
        return (float) lvl / max;
    }

    public static int getExpRequiredForLevel(int level) {
        return (int) (Math.pow(10F * GameBalanceConfig.get().NORMAL_STAT_SCALING.getMultiFor(level), 2.4F));
    }


    public static int getBaseExpMobReward(int level) {
        return 50 + scaleExpReward(4, level);
    }

    public static int scaleExpReward(int exp, int level) {
        return (int) (Math.pow(exp * GameBalanceConfig.get().NORMAL_STAT_SCALING.getMultiFor(level), 1.1F));
    }

    public static String OBELISK_DIM = "ancient_obelisks:obelisk";
    public static String HARVEST_DIM = "the_harvest:harvest";

    @Nullable
    private static Player getNearestPlayerInSameInstance(String dimid, Level world, BlockPos pos) {
        List<Player> players;
        if (dimid.equals(HARVEST_DIM)) {
            players = HarvestMain.HARVEST_MAP_STRUCTURE.getAllPlayersInMap(world, pos);
        } else if (dimid.equals(OBELISK_DIM)) {
            players = ObelisksMain.OBELISK_MAP_STRUCTURE.getAllPlayersInMap(world, pos);
        } else {
            return null;
        }

        if (players == null || players.isEmpty()) {
            return null;
        }

        var center = pos.getCenter();
        return players.stream().min(Comparator.comparingDouble(x -> x.distanceToSqr(center))).orElse(null);
    }

    private static int getLowestPartyMemberLevelNearby(Player nearestPlayer) {
        if (nearestPlayer == null) {
            return 1;
        }
        List<Player> partyMembers = TeamUtils.getOnlineTeamMembersInRange(nearestPlayer);

        if (partyMembers == null || partyMembers.isEmpty()) {
            // Not in a party, or no one in range, fallback to nearest player's level
            return Load.Unit(nearestPlayer).getLevel();
        }

        int minLevel = Load.Unit(nearestPlayer).getLevel();
        for (Player p : partyMembers) {
            int lvl = Load.Unit(p).getLevel();
            if (lvl < minLevel) {
                minLevel = lvl;
            }
        }

        return minLevel;
    }

    public static LevelInfo determineLevel(@Nullable LivingEntity en, Level world, BlockPos pos, @Nullable Player nearestPlayer, boolean usevariance) {

        LevelInfo info = new LevelInfo();

        ServerLevel sw = (ServerLevel) world;

        var opt = WorldUtils.ifMapData(world, pos);

        boolean scaletoPlayer = false;

        boolean ignoreEntityConfig = false;

        if (opt.isPresent()) {

            var data = opt.get();

            if (data != null) {
                info.set(LevelInfo.LevelSource.MAP_DIMENSION, data.map.getLevel());
                return info;
            } else {
                System.out.print("A mob spawned in a dungeon world without a dungeon data nearby!");
            }
        } else {
            // if the player enters side content not connected to map, it wont have data, so it should force the level to scale to player
            var dimid = MapManager.getResourceLocation((Level) world).toString();
            if (dimid.equals(OBELISK_DIM) || dimid.equals(HARVEST_DIM)) {
                scaletoPlayer = true;
                ignoreEntityConfig = true;

                // Harvest/Obelisk instances are tiled a few chunks apart inside ONE shared dimension
                // (unlike dungeon_realm maps, which carry their own positional MapData), so the
                // dimension-wide nearest player can belong to someone else's concurrently running
                // instance. Prefer a player actually inside this instance.
                Player instancePlayer = getNearestPlayerInSameInstance(dimid, world, pos);
                if (instancePlayer != null) {
                    nearestPlayer = instancePlayer;
                }
            }
        }

        DimensionConfig dimConfig = ExileDB.getDimensionConfig(world);

        if (scaletoPlayer) {
            // Harvest/Obelisk mobs must always be leveled off a real player, never the distance/dimConfig
            // fallback below - falling through there is what produces wildly overleveled "skull" mobs.
            int lowestLevel = nearestPlayer != null ? getLowestPartyMemberLevelNearby(nearestPlayer) : dimConfig.min_lvl;
            info.set(LevelInfo.LevelSource.NEAREST_PLAYER_CONFIG, lowestLevel);
        } else if (ServerContainer.get().SCALE_MOB_LEVEL_TO_NEAREST_PLAYER.get() && nearestPlayer != null) {
            // Use the lowest party member level within PARTY_RADIUS
            int lowestLevel = getLowestPartyMemberLevelNearby(nearestPlayer);
            info.set(LevelInfo.LevelSource.NEAREST_PLAYER_CONFIG, lowestLevel);
        } else {
            if (isInMinLevelArea(sw, pos, dimConfig)) {
                info.set(LevelInfo.LevelSource.MIN_LEVEL_AREA, dimConfig.min_lvl);
            } else {
                if (dimConfig.scale_to_nearest_player && nearestPlayer != null) {
                    // Use the lowest party member level within PARTY_RADIUS
                    int lowestLevel = getLowestPartyMemberLevelNearby(nearestPlayer);
                    info.set(LevelInfo.LevelSource.NEAREST_PLAYER, lowestLevel);
                } else {
                    info.set(LevelInfo.LevelSource.DISTANCE_FROM_SPAWN, determineLevelPerDistanceFromSpawn(sw, pos, dimConfig));
                }
            }
        }

        if (usevariance) {
            var varianceConfig = ServerContainer.get().MOB_LEVEL_VARIANCE.get();
            int variance = RandomUtils.RandomRange(-varianceConfig, varianceConfig);
            info.add(LevelInfo.LevelSource.LEVEL_VARIANCE, variance);
        }
        info.capToRange(LevelInfo.LevelSource.DIMENSION, dimConfig.getLevelRangeFor(nearestPlayer));
        info.capToRange(LevelInfo.LevelSource.MAX_LEVEL, new MinMax(1, GameBalanceConfig.get().MAX_LEVEL));

        if (en != null) {
            if (!ignoreEntityConfig) {
                var enconfig = Load.Unit(en).getEntityConfig();
                info.capToRange(LevelInfo.LevelSource.ENTITY_CONFIG, new MinMax(enconfig.min_lvl, enconfig.max_lvl));
            }
        }

        return info;
    }

    public static boolean isInMinLevelArea(ServerLevel world, BlockPos pos, DimensionConfig config) {
        double distance = world.getSharedSpawnPos()
                .distManhattan(pos);

        double scale = Mth.clamp(world.dimensionType()
                .coordinateScale() / 3F, 1, Integer.MAX_VALUE);

        distance *= scale;

        if (distance < config.min_lvl_area) {
            return true;
        }
        return false;
    }

    public static int determineLevelPerDistanceFromSpawn(ServerLevel world, BlockPos pos, DimensionConfig config) {

        double distance = world.getSharedSpawnPos().distManhattan(pos);

        double scale = Mth.clamp(world.dimensionType().coordinateScale() / 3F, 1, Integer.MAX_VALUE);

        distance *= scale;

        if (distance < config.min_lvl_area) {
            return config.min_lvl;
        }

        int lvl = 1;

        lvl = (int) (config.min_lvl + ((distance - config.min_lvl_area) / (config.mob_lvl_per_distance)));

        return Mth.clamp(lvl, config.min_lvl, config.max_lvl);

    }


}