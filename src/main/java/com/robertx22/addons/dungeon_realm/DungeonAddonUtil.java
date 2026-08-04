package com.robertx22.addons.dungeon_realm;

import com.robertx22.dungeon_realm.database.data_blocks.mobs.SpawnPointHelper;
import com.robertx22.mine_and_slash.database.data.rarities.MobRarity;
import com.robertx22.mine_and_slash.event_hooks.entity.OnMobSpawn;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class DungeonAddonUtil {

    // how far from the encounter block we're willing to look for a spot, and which floor levels to
    // consider - one up for a step, one down for a drop
    private static final int SPAWN_SEARCH_RADIUS = 3;
    private static final int[] SPAWN_SEARCH_HEIGHTS = {0, 1, -1};

    public static Consumer<LivingEntity> createMobRarityEdit(MobRarity rarity) {
        return mob -> {
            if (rarity != null) {
                Load.Unit(mob).setRarity(rarity.GUID());
            }
            OnMobSpawn.setupNewMobOnSpawn(mob);
            if (rarity != null) {
                Load.Unit(mob).setRarity(rarity.GUID());
            }
        };
    }

    /**
     * Positions a bonus-encounter mob (Strongbox guardian, Imprisoned Monster captive) somewhere it
     * actually fits, before it's added to the level.
     * <p>
     * The encounters used to place mobs at "encounter pos + random offset, at y + 1" with no space
     * check at all. y + 1 is the layer the encounter's own {@link BonusEncounterTopperBlock} occupies
     * - a full solid cube - so a good share of every pack spawned straight into it, and the rest could
     * land in the room's walls.
     * <p>
     * Callers should still run UnstuckMobs.unstuckFromWalls after addFreshEntity, the same way regular
     * dungeon mobs are handled in PREPARE_DUNGEON_MOB_SPAWN, as a last resort for the rooms where
     * nothing here works out.
     */
    public static void placeEncounterMob(ServerLevel level, Mob mob, BlockPos origin, RandomSource random) {
        // jitter the search start: SpawnPointHelper is a deterministic BFS, so seeding every guardian
        // of a pack from the encounter block itself would resolve all of them onto one spot
        BlockPos start = origin.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
        if (tryPlace(level, mob, SpawnPointHelper.getBestSpawnPosition(level, start), random)) {
            return;
        }

        // that spot is taken or only partly clear - walk the area around the encounter looking for
        // anywhere this particular mob's bounding box fits
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -SPAWN_SEARCH_RADIUS; dx <= SPAWN_SEARCH_RADIUS; dx++) {
            for (int dz = -SPAWN_SEARCH_RADIUS; dz <= SPAWN_SEARCH_RADIUS; dz++) {
                if (dx == 0 && dz == 0) {
                    continue; // the encounter block's own column, where the topper sits
                }
                for (int dy : SPAWN_SEARCH_HEIGHTS) {
                    candidates.add(origin.offset(dx, dy, dz));
                }
            }
        }
        Collections.shuffle(candidates);

        for (BlockPos candidate : candidates) {
            if (tryPlace(level, mob, candidate, random)) {
                return;
            }
        }

        // nothing nearby fits. put it at the encounter's own floor level rather than leaving it at
        // wherever the last candidate was, and let the caller's unstuck pass deal with it
        moveTo(mob, origin, random);
    }

    private static boolean tryPlace(ServerLevel level, Mob mob, BlockPos pos, RandomSource random) {
        if (!SpawnPointHelper.doesBlockBlockFeet(level, pos.below())) {
            return false; // nothing to stand on - don't drop a guardian into a pit
        }
        moveTo(mob, pos, random);
        // noCollision sees the guardians already added to the level too, so a pack spreads out
        // instead of stacking into one block
        return level.noCollision(mob);
    }

    private static void moveTo(Mob mob, BlockPos pos, RandomSource random) {
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360F, 0);
    }
}
