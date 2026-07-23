package com.robertx22.addons.dungeon_realm;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.addon.ExtendedOrb;
import com.robertx22.dungeon_realm.capability.DungeonEntityCapability;
import com.robertx22.dungeon_realm.configs.DungeonConfig;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.database.mob_list.MobList;
import com.robertx22.library_of_exile.registry.IWeighted;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import com.robertx22.mine_and_slash.database.data.gear_types.bases.BaseGearType;
import com.robertx22.mine_and_slash.database.data.omen.OmenBlueprint;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.StrongboxExtraDrops;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.StrongboxGuardianToughness;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.StrongboxUniqueChance;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.MasterLootGen;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.JewelBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.RuneBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.SkillGemBlueprint;
import com.robertx22.mine_and_slash.loot.generators.GemLootGen;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// A locked "strongbox" bonus-map encounter, placed by the MapContent system (MnsMapContents.STRONGBOX).
// Right-clicking it releases a guardian pack drawn from the current dungeon's own mob pool; the box
// stays sealed until every guardian is dead, then spills a richer-than-normal loot reward and consumes
// itself. Lives in the dungeon-realm glue package so it can use the dungeon mob pool directly.
public class StrongboxBlock extends BaseEntityBlock {

    private static final UUID GUARDIAN_TOUGHNESS_HP_MOD = UUID.fromString("6f2b6a5e-9d3a-4b3a-9f0a-1c2b3d4e5f6a");
    private static final UUID GUARDIAN_TOUGHNESS_DMG_MOD = UUID.fromString("7a3c7b6f-ae4b-4c4b-a01b-2d3c4e5f6a7b");

    public StrongboxBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.CHEST).noOcclusion().lightLevel(x -> 10));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StrongboxBE(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 6; i++) {
            level.addParticle(ParticleTypes.ANGRY_VILLAGER,
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
        if (!(level.getBlockEntity(pos) instanceof StrongboxBE be)) {
            return InteractionResult.SUCCESS;
        }
        if (be.activated) {
            // already opened - sealed until the guardians are dealt with
            return InteractionResult.SUCCESS;
        }
        spawnGuardians((ServerLevel) level, pos, p, be);
        be.activated = true;
        be.setChanged();
        SoundUtils.playSound(level, pos, SoundEvents.CHEST_OPEN);
        return InteractionResult.SUCCESS;
    }

    private void spawnGuardians(ServerLevel level, BlockPos pos, Player p, StrongboxBE be) {
        MobList mobList = null;
        try {
            // the current dungeon's own mob pool, so guardians match the map they're in
            mobList = DungeonMain.DUNGEON_MOB_SPAWNS.getPredeterminedRandom(level, pos);
        } catch (Exception ignored) {
            // not inside a dungeon map (e.g. creative placement) - fall back below
        }
        RandomSource random = level.random;
        int spawned = 0;
        int guardianCount = DungeonConfig.get().STRONGBOX_GUARDIAN_COUNT.get();
        // Atlas "Unique Windfall" - guardians hit harder and have more health, without changing rarity
        float toughnessBonus = Load.Unit(p).getUnit().getCalculatedStat(StrongboxGuardianToughness.getInstance()).getValue();
        for (int i = 0; i < guardianCount; i++) {
            EntityType<?> type = mobList != null ? mobList.getRandomMob().getType() : EntityType.ZOMBIE;
            Entity entity = type.create(level);
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 3;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 3;
            mob.moveTo(x, pos.getY() + 1, z, random.nextFloat() * 360F, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            mob.setPersistenceRequired();
            mob.setTarget(p);
            applyGuardianToughness(mob, toughnessBonus);
            level.addFreshEntity(mob);
            // count guardians toward map completion: flag as a dungeon mob so their deaths register
            // as mobKills (see DungeonEvents), and add them to mobSpawnCount below (the denominator).
            var entityData = DungeonEntityCapability.get(mob).data;
            entityData.isDungeonMob = true;
            // tag this mob as belonging to this box so a LivingDeathEvent hook (DungeonAddonEvents)
            // can decrement guardiansRemaining on death - robust even if the guardian's chunk unloads,
            // unlike polling isAlive() by UUID.
            entityData.isStrongboxGuardian = true;
            entityData.strongboxPos = pos.asLong();
            be.guardiansRemaining++;
            spawned++;
        }
        int finalSpawned = spawned;
        DungeonMain.ifMapData(level, pos).ifPresent(x -> {
            x.mobSpawnCount += finalSpawned;
            x.updateMapCompletionRarity(level, pos);
        });
    }

    private void applyGuardianToughness(Mob mob, float toughnessBonus) {
        if (toughnessBonus <= 0) {
            return;
        }
        AttributeInstance maxHealthAttribute = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealthAttribute.addPermanentModifier(new AttributeModifier(
                    GUARDIAN_TOUGHNESS_HP_MOD, "Strongbox guardian toughness",
                    toughnessBonus / 100F, AttributeModifier.Operation.MULTIPLY_TOTAL));
            mob.setHealth((float) maxHealthAttribute.getValue());
        }
        AttributeInstance attackDamageAttribute = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttribute != null) {
            attackDamageAttribute.addPermanentModifier(new AttributeModifier(
                    GUARDIAN_TOUGHNESS_DMG_MOD, "Strongbox guardian toughness",
                    toughnessBonus / 100F, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof StrongboxBE sbe && sbe.activated) {
                // check for unlock once a second - click each check like a ticking timer, even
                // though there's no actual time limit, so players get audible feedback it's "watching".
                if (sbe.tick++ % 20 == 0) {
                    if (sbe.guardiansRemaining <= 0) {
                        unlock((ServerLevel) lvl, pos, sbe);
                    } else {
                        SoundUtils.playSound(lvl, pos, SoundEvents.LEVER_CLICK);
                    }
                }
            }
        };
    }

    private void unlock(ServerLevel level, BlockPos pos, StrongboxBE be) {
        // reward whoever is present (the party member who cleared the guards)
        Player recipient = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 48, false);
        if (recipient != null) {
            int lootRolls = DungeonConfig.get().STRONGBOX_LOOT_ROLLS.get();
            for (int i = 0; i < lootRolls; i++) {
                LootInfo info = LootInfo.ofChestLoot(recipient, pos);
                List<ItemStack> items = MasterLootGen.generateLoot(info);
                for (ItemStack stack : items) {
                    Block.popResource(level, pos, stack);
                }
            }

            // on top of the normal chest rolls, a strongbox always pays out a handful of items each
            // independently rolled from a weighted category - this is what makes it feel distinct
            // from a bigger normal chest
            int categoryItemCount = RandomUtils.RandomRange(
                    DungeonConfig.get().STRONGBOX_CATEGORY_ITEM_MIN.get(),
                    DungeonConfig.get().STRONGBOX_CATEGORY_ITEM_MAX.get());
            // Atlas "Strongbox Extra Drops" - scales the guaranteed category-item count
            float extraDropsMulti = Load.Unit(recipient).getUnit().getCalculatedStat(StrongboxExtraDrops.getInstance()).getMultiplier();
            categoryItemCount = Math.round(categoryItemCount * extraDropsMulti);
            // Atlas "Unique Windfall" - scales the UNIQUE category's odds within the weighted roll
            float uniqueChanceBonus = Load.Unit(recipient).getUnit().getCalculatedStat(StrongboxUniqueChance.getInstance()).getValue();
            List<ScaledCategory> weightedCategories = Arrays.stream(LootCategory.values())
                    .map(c -> new ScaledCategory(c, c == LootCategory.UNIQUE
                            ? Math.round(c.Weight() * (1F + uniqueChanceBonus / 100F))
                            : c.Weight()))
                    .collect(Collectors.toList());
            for (int i = 0; i < categoryItemCount; i++) {
                LootCategory category = RandomUtils.weightedRandom(weightedCategories).category;
                ItemStack categoryItem = generateCategoryItem(category, LootInfo.ofChestLoot(recipient, pos));
                if (!categoryItem.isEmpty()) {
                    Block.popResource(level, pos, categoryItem);
                }
            }
        }
        SoundUtils.playSound(level, pos, SoundEvents.PLAYER_LEVELUP);
        level.removeBlock(pos, false);
    }

    private enum LootCategory implements IWeighted {
        CURRENCY(() -> DungeonConfig.get().STRONGBOX_CATEGORY_WEIGHT_CURRENCY.get()),
        WEAPON(() -> DungeonConfig.get().STRONGBOX_CATEGORY_WEIGHT_WEAPON.get()),
        ARMOR(() -> DungeonConfig.get().STRONGBOX_CATEGORY_WEIGHT_ARMOR.get()),
        JEWELRY(() -> DungeonConfig.get().STRONGBOX_CATEGORY_WEIGHT_JEWELRY.get()),
        JEWEL(() -> DungeonConfig.get().STRONGBOX_CATEGORY_WEIGHT_JEWEL.get()),
        OMEN(() -> DungeonConfig.get().STRONGBOX_CATEGORY_WEIGHT_OMEN.get()),
        SUPPORT_GEM(() -> DungeonConfig.get().STRONGBOX_CATEGORY_WEIGHT_SUPPORT_GEM.get()),
        AURA_GEM(() -> DungeonConfig.get().STRONGBOX_CATEGORY_WEIGHT_AURA_GEM.get()),
        RUNE(() -> DungeonConfig.get().STRONGBOX_CATEGORY_WEIGHT_RUNE.get()),
        SOCKETABLE_GEM(() -> DungeonConfig.get().STRONGBOX_CATEGORY_WEIGHT_SOCKETABLE_GEM.get()),
        UNIQUE(() -> DungeonConfig.get().STRONGBOX_CATEGORY_WEIGHT_UNIQUE.get());

        private final java.util.function.IntSupplier weightSupplier;

        LootCategory(java.util.function.IntSupplier weightSupplier) {
            this.weightSupplier = weightSupplier;
        }

        @Override
        public int Weight() {
            return weightSupplier.getAsInt();
        }
    }

    // wraps a LootCategory with a per-roll-scaled weight (Atlas "Unique Windfall"), so the UNIQUE
    // category's odds can be boosted for a specific opener without touching the enum's base weights
    private static class ScaledCategory implements IWeighted {
        final LootCategory category;
        final int weight;

        ScaledCategory(LootCategory category, int weight) {
            this.category = category;
            this.weight = weight;
        }

        @Override
        public int Weight() {
            return weight;
        }
    }

    private ItemStack generateCategoryItem(LootCategory category, LootInfo info) {
        switch (category) {
            case CURRENCY: {
                ExileCurrency currency = LibDatabase.Currency()
                        .getFilterWrapped(x -> {
                            var ext = ExtendedOrb.from(x);
                            if (ext != null) {
                                if (ext.drop_req.hasLeague() && !ext.drop_req.canDropInLeague(info.league, info.level)) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .random();
                return currency.getItem().getDefaultInstance();
            }
            case WEAPON: {
                GearBlueprint b = new GearBlueprint(info);
                b.gearItemSlot.override(ExileDB.GearTypes().getFilterWrapped(BaseGearType::isWeapon).random());
                return b.createStack();
            }
            case ARMOR: {
                GearBlueprint b = new GearBlueprint(info);
                b.gearItemSlot.override(ExileDB.GearTypes().getFilterWrapped(BaseGearType::isArmor).random());
                return b.createStack();
            }
            case JEWELRY: {
                GearBlueprint b = new GearBlueprint(info);
                b.gearItemSlot.override(ExileDB.GearTypes().getFilterWrapped(BaseGearType::isJewelry).random());
                return b.createStack();
            }
            case UNIQUE: {
                GearBlueprint b = new GearBlueprint(info);
                b.rarity.override(ExileDB.GearRarities().get(IRarity.UNIQUE_ID));
                return b.createStack();
            }
            case JEWEL:
                return new JewelBlueprint(info).createStack();
            case OMEN:
                return new OmenBlueprint(info).createStack();
            case SUPPORT_GEM:
                return new SkillGemBlueprint(info, SkillGemData.SkillGemType.SUPPORT).createStack();
            case AURA_GEM:
                return new SkillGemBlueprint(info, SkillGemData.SkillGemType.AURA).createStack();
            case RUNE:
                return new RuneBlueprint(info).createStack();
            case SOCKETABLE_GEM:
                return GemLootGen.droppableAtLevel(info.level).random().getItem().getDefaultInstance();
            default:
                return ItemStack.EMPTY;
        }
    }
}
