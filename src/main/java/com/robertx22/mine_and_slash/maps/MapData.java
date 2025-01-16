package com.robertx22.mine_and_slash.maps;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.league.LeagueMechanics;
import com.robertx22.mine_and_slash.database.data.league.LeagueStructure;
import com.robertx22.mine_and_slash.database.data.profession.ExplainedResult;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.maps.spawned_map_mobs.SpawnedMobList;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;

import java.util.HashMap;
import java.util.stream.Collectors;

public class MapData {


    public MapItemData map = new MapItemData();

    public MapRoomsData rooms = new MapRoomsData();

    public MapLeaguesData leagues = new MapLeaguesData();

    private HashMap<String, Integer> lives = new HashMap<>();

    public String dungeonid = "";

    public String mobs = "";

    public String completion_rarity = IRarity.COMMON_ID;

    public boolean gave_boss_tp = false;

    public int despawnedMobs = 0;

    public SpawnedMobList getMobSpawns() {
        return ExileDB.MapMobs().get(mobs);
    }

    public int getLives(Player p) {
        int cur = lives.getOrDefault(p.getStringUUID(), map.getRarity().map_lives);
        return cur;
    }

    public void reduceLives(Player p) {
        int cur = getLives(p) - 1;
        lives.put(p.getStringUUID(), cur);
    }

    public String playerUuid = "";

    public int chunkX = 0;
    public int chunkZ = 0;


    public static MapData newMap(Player p, MapItemData map, MapsData maps) {

        Load.player(p).prophecy.affixesTaken.clear();

        maps.deleteOldMap(p);

        MapData data = new MapData();
        data.playerUuid = p.getStringUUID();
        data.map = map;

        ChunkPos cp = data.randomFree(p.level(), maps);

        data.chunkX = cp.x;
        data.chunkZ = cp.z;

        data.leagues.setupOnMapStart(map, p);

        maps.addToList(cp);

        return data;

    }

    public void spawnRandomLeagueMechanic(Level level, BlockPos pos) {
        var league = LeagueStructure.getMechanicFromPosition((ServerLevel) level, pos);

        if (league == LeagueMechanics.NONE) {
            var list = leagues.getLeagueMechanics().stream().filter(x -> leagues.get(x).remainingSpawns > 0).collect(Collectors.toList());

            if (list.size() > 0) {
                var randomLeague = RandomUtils.randomFromList(list);
                randomLeague.spawnMechanicInMap((ServerLevel) level, pos);
                leagues.get(randomLeague).remainingSpawns--;
            }
        }
    }


    public void teleportToMap(Player p) {
        if (p.level().isClientSide) {
            return;
        }


        Load.player(p).map.sendMapTpMsg = true;

        Load.player(p).map.tpbackdim = p.level().dimension().location().toString();
        Load.player(p).map.tp_back_pos = p.blockPosition().asLong();


        BlockPos pos = getDungeonStartTeleportPos(new ChunkPos(this.chunkX, this.chunkZ));

        Level world = p.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, WorldUtils.DUNGEON_DIM_ID));


        world.setBlock(new BlockPos(pos.getX(), 54, pos.getZ()), Blocks.BEDROCK.defaultBlockState(), 2);

        TeleportUtils.teleport((ServerPlayer) p, pos, WorldUtils.DUNGEON_DIM_ID);


    }


    public static BlockPos getDungeonStartTeleportPos(ChunkPos pos) {
        BlockPos p = getStartChunk(pos, MapData.DUNGEON_LENGTH).getBlockAt(0, 0, 0);
        p = new BlockPos(p.getX() + 8, 57, p.getZ() + 8);
        return p;
    }


    private ChunkPos randomFree(Level level, MapsData maps) {

        int MAXTRIES = 300;

        ChunkPos pos = null;

        int tries = 0;

        // seems this is how you get the level
        WorldBorder border = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, WorldUtils.DUNGEON_DIM_ID)).getWorldBorder();

        int max = (int) (border.getMaxX() / 16);

        max = MathHelper.clamp(max, 0, Integer.MAX_VALUE);

        while (pos == null || maps.startExistsAlready(pos) || !border.isWithinBounds(pos)) {
            if (tries++ > MAXTRIES) {
                ExileLog.get().warn("Tried too many times to find random dungeon pos and failed, please delete the map dimension folder");
                return null;
            }
            int x = RandomUtils.RandomRange(50, max);
            int z = RandomUtils.RandomRange(50, max);

            pos = new ChunkPos(x, z);
            pos = getStartChunk(pos.getMiddleBlockPosition(50), MapData.DUNGEON_LENGTH);

        }

        if (tries > MAXTRIES) {
            ExileLog.get().warn("It took more than " + MAXTRIES + "  tries to find random free dungeon, either you are insanely unlucky, or the world is close to filled! Dungeon worlds are cleared on next server boot if they reach too close to capacity.");
        }

        return pos;

    }

    public ExplainedResult canTeleportToArena(Player p) {
        int perc = rooms.getMapCompletePercent();

        int needed = ServerContainer.get().MAP_PERCENT_COMPLETE_NEEDED_FOR_BOSS_ARENA.get().intValue();

        if (perc > needed) {
            return ExplainedResult.success(Chats.BOSS_ARENA_UNLOCKED.locName().withStyle(ChatFormatting.GREEN));
        } else {
            return ExplainedResult.failure(Chats.BOSS_LOCKED.locName(needed + "%").withStyle(ChatFormatting.RED));
        }
    }

    public static BlockPos getDungeonStartTeleportPos(BlockPos pos) {
        BlockPos p = getStartChunk(pos, MapData.DUNGEON_LENGTH).getMiddleBlockPosition(55);
        p = new BlockPos(p.getX(), 55, p.getZ());
        return p;
    }


    public boolean isMapHere(BlockPos pos) {
        var cp = getStartChunk(pos, MapData.DUNGEON_LENGTH);
        return cp.x == chunkX && cp.z == chunkZ;
    }

    public static int DUNGEON_LENGTH = 30;

    public static ChunkPos getStartChunk(BlockPos pos, int offset) {
        return getStartChunk(new ChunkPos(pos), offset);
    }

    // todo why the hell does this only work fine with the 11 minus
    public static ChunkPos getStartChunk(ChunkPos cp, int offset) {
        // var newcp = (new ChunkPos(cp.x / offset * offset, cp.z / offset * offset));

        //return newcp;


        int chunkX = cp.x;
        int chunkZ = cp.z;
        int distToEntranceX = 11 - (chunkX % DUNGEON_LENGTH);
        int distToEntranceZ = 11 - (chunkZ % DUNGEON_LENGTH);
        chunkX += distToEntranceX;
        chunkZ += distToEntranceZ;
        return new ChunkPos(chunkX, chunkZ);


    }

}
