package com.robertx22.addons.dungeon_realm;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.ExileCurrencies;
import com.robertx22.dungeon_realm.capability.DungeonEntityCapability;
import com.robertx22.dungeon_realm.configs.DungeonConfig;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.mob_list.MobList;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.IdKey;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ImprisonedMonsterDoubleSpawn;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ImprisonedMonsterExtraDrops;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.generators.CurrencyLootGen;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

// An "imprisoned monster" bonus-map encounter (PoE Essence-style), placed by the MapContent system
// (MnsMapContents.IMPRISONED_MONSTER). Right-clicking it releases a single caged Boss-rarity mob drawn
// from the current dungeon's mob pool; when it dies, the encounter pays out a GUARANTEED reward
// (currency for now - a dedicated item type is planned). Lives in the glue package for direct
// dungeon-pool access.
public class ImprisonedMonsterBlock extends BaseEntityBlock {

    public ImprisonedMonsterBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).noOcclusion().lightLevel(x -> 10));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImprisonedMonsterBE(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 6; i++) {
            level.addParticle(ParticleTypes.SOUL,
                    pos.getX() + 0.6 + (random.nextDouble() - 0.5) * 1.6,
                    pos.getY() + 1.0 + random.nextDouble() * 0.5,
                    pos.getZ() + 0.6 + (random.nextDouble() - 0.5) * 1.6,
                    0, 0.02, 0);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ImprisonedMonsterBE be)) {
            return InteractionResult.SUCCESS;
        }
        if (be.activated) {
            // already released - the reward only comes once the monster is slain
            return InteractionResult.SUCCESS;
        }
        spawnMonster((ServerLevel) level, pos, p, be);
        be.activated = true;
        be.setChanged();
        SoundUtils.playSound(level, pos, SoundEvents.WITHER_SPAWN);
        return InteractionResult.SUCCESS;
    }

    private void spawnMonster(ServerLevel level, BlockPos pos, Player p, ImprisonedMonsterBE be) {
        MobList mobList = null;
        try {
            mobList = DungeonMain.DUNGEON_MOB_SPAWNS.getPredeterminedRandom(level, pos);
        } catch (Exception ignored) {
            // not inside a dungeon map (e.g. creative placement) - fall back below
        }

        // Atlas "Twin Captives" - releases 2 caged mobs instead of 1
        boolean doubleSpawn = Load.Unit(p).getUnit().getCalculatedStat(ImprisonedMonsterDoubleSpawn.getInstance()).getValue() > 0;
        int monsterCount = doubleSpawn ? 2 : 1;

        int spawned = 0;
        for (int i = 0; i < monsterCount; i++) {
            RandomSource random = level.random;
            EntityType<?> type = mobList != null ? mobList.getRandomMob().getType() : EntityType.ZOMBIE;
            Entity entity = type.create(level);
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2;
            mob.moveTo(x, pos.getY() + 1, z, random.nextFloat() * 360F, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            level.addFreshEntity(mob);

            // make it a real mini-boss: force Boss rarity (re-runs stat setup for that rarity)
            var bossRarity = ExileDB.MobRarities().get(IRarity.BOSS);
            DungeonAddonUtil.createMobRarityEdit(bossRarity).accept(mob);
            // re-roll affixes for the forced rarity - createMobRarityEdit runs after the mob's automatic
            // first spawn pass, which already rolled (and skipped-on-override) affixes for a random rarity
            Load.Unit(mob).getAffixData().randomizeAffixes(bossRarity);

            mob.setPersistenceRequired();
            mob.setTarget(p);

            // count toward map completion as a mini-boss (deaths -> miniBossKills; miniBossSpawnCount
            // below is the denominator). Mini-boss weight (see DungeonConfig.MINI_BOSS_COMPLETION_WEIGHT)
            // makes it a meaningful chunk of progress, matching the harder fight.
            DungeonEntityCapability.get(mob).data.isMiniBossMob = true;
            be.monsters.add(mob.getUUID());
            spawned++;
        }

        int finalSpawned = spawned;
        DungeonMain.ifMapData(level, pos).ifPresent(x -> {
            x.miniBossSpawnCount += finalSpawned;
            x.updateMapCompletionRarity(level, pos);
        });
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof ImprisonedMonsterBE mbe && mbe.activated) {
                if (mbe.tick++ % 20 == 0 && monsterDead((ServerLevel) lvl, mbe)) {
                    reward((ServerLevel) lvl, pos, mbe);
                }
            }
        };
    }

    private static boolean monsterDead(ServerLevel level, ImprisonedMonsterBE be) {
        if (be.monsters.isEmpty()) {
            return true;
        }
        for (UUID id : be.monsters) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive()) {
                return false;
            }
        }
        return true;
    }

    private void reward(ServerLevel level, BlockPos pos, ImprisonedMonsterBE be) {
        Player recipient = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 48, false);
        if (recipient != null) {
            LootInfo info = LootInfo.ofChestLoot(recipient, pos);
            CurrencyLootGen gen = new CurrencyLootGen(info);
            // Atlas "Imprisoned Monster Extra Drops" - scales the guaranteed currency count
            float extraDropsMulti = Load.Unit(recipient).getUnit().getCalculatedStat(ImprisonedMonsterExtraDrops.getInstance()).getMultiplier();
            // "Twin Captives" already doubled how many monsters were caged - scale the combined
            // payout by that same count so 2 captives pay out 2x total, not a separate full reward each
            int monsterCount = Math.max(1, be.monsters.size());
            int rewardCount = Math.round(DungeonConfig.get().IMPRISONED_MONSTER_CURRENCY_REWARD.get() * extraDropsMulti * monsterCount);
            for (int i = 0; i < rewardCount; i++) {
                ItemStack currency = gen.generateOne();
                if (currency != null && !currency.isEmpty()) {
                    Block.popResource(level, pos, currency);
                }
            }
            for (int i = 0; i < monsterCount; i++) {
                dropPotentialSeed(level, pos, info.level);
            }
        }
        SoundUtils.playSound(level, pos, SoundEvents.PLAYER_LEVELUP);
        level.removeBlock(pos, false);
    }

    // chance-based bonus drop: a "Seed" currency that restores Potential to a piece of gear, once.
    // Slot is picked uniformly at random; the rarer Perfected tier only becomes possible once the
    // loot level meets DungeonConfig.MIN_LEVEL_FOR_PERFECTED_SEED, below which only base Seeds drop.
    // These currencies are exclusive to this encounter (registered with loot-weight 0).
    private void dropPotentialSeed(ServerLevel level, BlockPos pos, int lootLevel) {
        DungeonConfig config = DungeonConfig.get();
        if (!RandomUtils.roll(config.IMPRISONED_MONSTER_SEED_DROP_CHANCE.get())) {
            return;
        }
        var currencies = ExileCurrencies.INSTANCE.IMPRISONED_MONSTER;
        boolean perfectedEligible = lootLevel >= config.MIN_LEVEL_FOR_PERFECTED_SEED.get();
        List<ExileKey<ExileCurrency, IdKey>> pool =
                (perfectedEligible && RandomUtils.roll(config.IMPRISONED_MONSTER_PERFECTED_SEED_CHANCE.get()))
                        ? currencies.PERFECTED_SEEDS : currencies.BASE_SEEDS;
        var seed = pool.get(level.random.nextInt(pool.size()));
        Block.popResource(level, pos, new ItemStack(seed.getItem()));
    }
}
