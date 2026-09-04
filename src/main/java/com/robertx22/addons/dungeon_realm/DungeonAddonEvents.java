package com.robertx22.addons.dungeon_realm;

import com.robertx22.dungeon_realm.api.*;
import com.robertx22.dungeon_realm.capability.DungeonEntityCapability;
import com.robertx22.dungeon_realm.capability.DungeonEntityData;
import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.database.atlas.AtlasNode;
import com.robertx22.dungeon_realm.database.atlas.AtlasNodeUtils;
import com.robertx22.dungeon_realm.database.holders.DungeonMapBlocks;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.library_of_exile.dimension.MapDimensions;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.main.ApiForgeEvents;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.vanilla_mc.packets.OpenGuiPacket;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.characters.PlayerStats;
import com.robertx22.mine_and_slash.capability.world.WorldData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.atlas.AtlasNodeLayout;
import com.robertx22.mine_and_slash.database.data.game_balance_config.PlayerPointsType;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.AdditionalBossChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.DoubleEventChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.DuplicateMapChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.EventFocusPenalty;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.HarvestEventChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ImprisonedMonsterEventChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ObeliskEventChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.PackSize;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ProphecyEventChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.RelicFind;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ShrineEventChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.StrongboxEventChance;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.UberFragmentFind;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.MapBlueprint;
import com.robertx22.mine_and_slash.loot.league.LootLeagueResolver;
import com.robertx22.mine_and_slash.loot.league.LootLeagueResolvers;
import com.robertx22.mine_and_slash.maps.MapData;
import com.robertx22.mine_and_slash.maps.MapItemData;
import com.robertx22.mine_and_slash.uncommon.ExplainedResultUtil;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.OnScreenMessageUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.List;

public class DungeonAddonEvents {

    public static void init() {

        // WorldData.map holds the MapData whose level every mob in a map (and in any side content
        // connected to it) is spawned at. dungeon_realm's clearMapDataOnFolderWipe only knows about its
        // own store, so this one used to survive the wipe while the instance counter restarted at 0 -
        // a brand new map then read back a previous session's MapData under the recycled coordinates,
        // which is how players ran into mobs far above their level.
        DungeonMain.MAP.addOnWipeListener(server -> WorldData.get(server.overworld()).map.clearAll());

        // Strongbox guardian death tracking (StrongboxBlock/StrongboxBE): decrement the owning
        // box's persisted guardiansRemaining counter here, on the actual death event, rather than
        // having the box poll isAlive() by UUID - that would falsely read "dead" for a guardian
        // whose chunk has simply unloaded while the box's own chunk is still loaded and ticking.
        ApiForgeEvents.registerForgeEvent(LivingDeathEvent.class, event -> {
            LivingEntity mob = event.getEntity();
            if (mob.level().isClientSide) {
                return;
            }
            // this fires for every death in every dimension, so bail out before touching the
            // capability - DungeonEntityCapability.get() ends in .orElse(new DungeonEntityCapability(..)),
            // which allocates a throwaway capability + data object on every miss. Both encounters
            // only ever exist inside a dungeon map.
            if (!MapDimensions.isMap(mob.level())) {
                return;
            }
            DungeonEntityData data = DungeonEntityCapability.get(mob).data;
            if (data.isStrongboxGuardian) {
                BlockPos boxPos = BlockPos.of(data.strongboxPos);
                if (mob.level().getBlockEntity(boxPos) instanceof StrongboxBE be) {
                    be.guardiansRemaining = Math.max(0, be.guardiansRemaining - 1);
                    be.setChanged();
                }
            }
            if (data.isImprisonedMonster) {
                BlockPos cagePos = BlockPos.of(data.imprisonedMonsterPos);
                if (mob.level().getBlockEntity(cagePos) instanceof ImprisonedMonsterBE be) {
                    be.monstersRemaining = Math.max(0, be.monstersRemaining - 1);
                    be.setChanged();
                }
            }
        });

        // Tell the loot system which league a dungeon-side kill belongs to, so UniqueGear tagged
        // "uber"/"pinnacle"/"strongbox"/"imprisoned_monster" drops from those encounters and nowhere
        // else. None of these leagues has spatial bounds, so LootInfo's position lookup can't find them.
        // Deliberately never returns PROPHECY: prophecy is a curse on the whole map, and its uniques
        // come only from the prophecy reward roll (GearProphecy), never from cursed mobs.
        LootLeagueResolvers.register(new LootLeagueResolver() {
            @Override
            public int priority() {
                return LootLeagueResolver.MOB_TAG;
            }

            @Override
            public League resolve(LootInfo info) {
                if (info.mobKilled == null) {
                    return null;
                }
                // rarity first: no capability lookup, and it's how WatcherEyeLootGen/PinnacleGemLootGen
                // already identify these bosses
                if (info.mobData != null) {
                    String rarity = info.mobData.getMobRarity().GUID();
                    if (IRarity.PINNACLE.equals(rarity)) {
                        return MnsLeagues.INSTANCE.PINNACLE.get();
                    }
                    if (IRarity.UBER.equals(rarity)) {
                        return MnsLeagues.INSTANCE.UBER.get();
                    }
                }
                // same reason as the LivingDeathEvent hook above: DungeonEntityCapability.get() ends in
                // .orElse(new ..) and allocates on every miss, and this runs for every mob killed in
                // every dimension. All of these only exist inside a dungeon map.
                if (!MapDimensions.isMap(info.mobKilled.level())) {
                    return null;
                }
                DungeonEntityData data = DungeonEntityCapability.get(info.mobKilled).data;
                if (data.isImprisonedMonster) {
                    return MnsLeagues.INSTANCE.IMPRISONED_MONSTER.get();
                }
                if (data.isStrongboxGuardian) {
                    return MnsLeagues.INSTANCE.STRONGBOX.get();
                }
                if (data.isPinnacleBoss) {
                    return MnsLeagues.INSTANCE.PINNACLE.get();
                }
                if (data.isUberBoss) {
                    return MnsLeagues.INSTANCE.UBER.get();
                }
                return null;
            }
        });

        DungeonExileEvents.ON_GENERATE_NEW_MAP_ITEM.register(new EventConsumer<OnGenerateNewMapItemEvent>() {
            @Override
            public void accept(OnGenerateNewMapItemEvent event) {
                // use a player-aware LootInfo so player-driven map rolls (Atlas map_rarity_bias) apply;
                // fall back to the dummy level-1 context when there's no player to roll for.
                LootInfo info = event.player != null ? LootInfo.ofPlayer(event.player) : LootInfo.ofLevel(1);
                MapBlueprint b = new MapBlueprint(info);
                // this runs on a fresh blueprint, so the caller's intent only reaches createData()
                // through the settings object. Defaults to no inheritance if nobody opted in.
                b.inheritMapTier = event.settings.inheritMapTier;
                b.fromMapBoss = event.settings.fromMapBoss;
                StackSaving.MAP.saveTo(event.mapStack, b.createData());
            }
        });

        DungeonExileEvents.ON_START_NEW_MAP.register(new EventConsumer<OnStartMapEvent>() {
            @Override
            public void accept(OnStartMapEvent event) {

                if (event.mapInfo.dimensionId.equals(DungeonMain.DIMENSION_KEY)) {

                    var map = StackSaving.MAP.loadFrom(event.stack);

                    if (map != null) {

                        map.lvl = Load.Unit(event.p).getLevel();

                        var mapdata = MapData.newMap(event.p, map);

                        // Uber/Pinnacle maps are exempt from Entry Tickets. Captured now, off the
                        // item that is about to be consumed - the same `uber || pinnacle` test
                        // MapBonusContentsData uses to give this instance its boss arena.
                        var dungeonItem = DungeonItemNbt.DUNGEON_MAP.loadFrom(event.stack);
                        if (dungeonItem != null && (dungeonItem.uber || dungeonItem.pinnacle)) {
                            mapdata.unlimitedEntries = true;
                        }

                        WorldData.get(event.p.level()).map.setData(event.p, mapdata, event.mapInfo.structure, event.startChunkPos.getMiddleBlockPosition(5));

                        Load.Unit(event.p).getCooldowns().setOnCooldown("start_map", ServerContainer.get().MAP_START_COOLDOWN_SECONDS.get() * 20);
                    }
                }
            }
        });

        DungeonExileEvents.ON_SPAWN_UBER_BOSS.register(new EventConsumer<SpawnUberEvent>() {
            @Override
            public void accept(SpawnUberEvent event) {
                LivingEntity en = event.uberBoss;
                Load.Unit(en).setRarity(IRarity.UBER);
                Load.Unit(en).recalcStats_DONT_CALL();
            }
        });

        DungeonExileEvents.ON_SPAWN_PINNACLE_BOSS.register(new EventConsumer<SpawnPinnacleEvent>() {
            @Override
            public void accept(SpawnPinnacleEvent event) {
                LivingEntity en = event.pinnacleBoss;
                Load.Unit(en).setRarity(IRarity.PINNACLE);
                Load.Unit(en).recalcStats_DONT_CALL();
            }
        });

        DungeonExileEvents.PREPARE_DUNGEON_MOB_SPAWN.register(new EventConsumer<PrepareDungeonMobEditsEvent>() {
            @Override
            public void accept(PrepareDungeonMobEditsEvent event) {
                event.dataBlock.ifPresent(x -> {

                    if (DungeonMapBlocks.INSTANCE.ELITE_MOB.GUID().equals(x.GUID()) || DungeonMapBlocks.INSTANCE.ELITE_MOB_HORDE.GUID().equals(x.GUID())) {
                        var rar = ExileDB.MobRarities().getFilterWrapped(e -> e.is_elite).random();
                        event.edits.add(DungeonAddonUtil.createMobRarityEdit(rar));
                    }
                    if (DungeonMapBlocks.INSTANCE.MAP_BOSS.GUID().equals(x.GUID())) {
                        var rar = ExileDB.MobRarities().get(IRarity.BOSS);
                        event.edits.add(DungeonAddonUtil.createMobRarityEdit(rar));
                    }
                    if (DungeonMapBlocks.INSTANCE.BOSS.GUID().equals(x.GUID())) {
                        var rar = ExileDB.MobRarities().get(IRarity.MYTHIC_ID);
                        event.edits.add(DungeonAddonUtil.createMobRarityEdit(rar));
                    }

                });
                // put the mob somewhere it actually fits BEFORE it enters the level. MobBuilder.summon
                // drops every mob of a pack on the identical data block position, and this hook runs while
                // the mob is still detached, so the placement search also spreads a pack out.
                //
                // this used to be UnstuckMobs.unstuckFromWalls, whose failure branch killed the mob outright.
                // Nobody could see that death - the mob was not in the level yet - but its hurt and death
                // sounds carry by position and vanilla loot still dropped, which is the burst of invisible
                // deaths players heard on map entry, with no Mine and Slash drops because the mob's rarity
                // and stats are applied further down in summon(). It also left mobSpawnCount counting mobs
                // that could never be killed, so map completion could not reach 100%.
                if (event.mob instanceof Mob mob && mob.level() instanceof ServerLevel sl) {
                    DungeonAddonUtil.placeDungeonMob(sl, mob, mob.blockPosition(), mob.getRandom());
                }
            }
        });

        DungeonExileEvents.CAN_START_MAP.register(new EventConsumer<CanStartMapEvent>() {
            @Override
            public void accept(CanStartMapEvent event) {

                Player p = event.p;

                if (Load.Unit(p).getLevel() < ServerContainer.get().MIN_LEVEL_MAP_DROPS.get()) {
                    p.sendSystemMessage(ExplainedResultUtil.createErrorAndReason(Chats.MAP_DEVICE_USE_ERROR.locName(), Chats.TOO_LOW_LEVEL.locName(ServerContainer.get().MIN_LEVEL_MAP_DROPS.get())));
                    event.canEnter = false;
                    return;
                }
                MapItemData data = StackSaving.MAP.loadFrom(event.stack);

                if (data == null) {
                    p.sendSystemMessage(ExplainedResultUtil.createErrorAndReason(Chats.MAP_DEVICE_USE_ERROR, Chats.INVALID_MAP_ITEM));
                    event.canEnter = false;
                    return;
                }
                if (!checkCooldown(p)) {
                    event.canEnter = false;
                    return;
                }
                if (!meetsResists(p, data)) {
                    event.canEnter = false;
                    return;
                }

            }
        });
        DungeonExileEvents.GET_UNLOCKED_ATLAS_NODES.register(new EventConsumer<GetUnlockedAtlasNodesEvent>() {
            @Override
            public void accept(GetUnlockedAtlasNodesEvent event) {
                PlayerData pd = Load.player(event.player);
                pd.atlas.ensureInitialized();
                event.unlockedNodeIds = pd.atlas.unlockedNodes;
            }
        });

        DungeonExileEvents.GET_PACK_SIZE_BONUS.register(new EventConsumer<GetPackSizeBonusEvent>() {
            @Override
            public void accept(GetPackSizeBonusEvent event) {
                float max = 0;
                for (Player p : event.players) {
                    float value = Load.Unit(p).getUnit().getCalculatedStat(PackSize.getInstance()).getValue();
                    if (value > max) {
                        max = value;
                    }
                }
                event.bonusPercent = max;
            }
        });

        DungeonExileEvents.GET_EXTRA_MAP_BOSS_CHANCE.register(new EventConsumer<GetExtraMapBossChanceEvent>() {
            @Override
            public void accept(GetExtraMapBossChanceEvent event) {
                float max = 0;
                for (Player p : event.players) {
                    float value = Load.Unit(p).getUnit().getCalculatedStat(AdditionalBossChance.getInstance()).getValue();
                    if (value > max) {
                        max = value;
                    }
                }
                event.bonusPercent = max;
            }
        });

        DungeonExileEvents.GET_UBER_FRAGMENT_FIND_BONUS.register(new EventConsumer<GetUberFragmentFindBonusEvent>() {
            @Override
            public void accept(GetUberFragmentFindBonusEvent event) {
                event.bonusPercent = Load.Unit(event.player).getUnit().getCalculatedStat(UberFragmentFind.getInstance()).getValue();
            }
        });

        DungeonExileEvents.ANY_PINNACLE_UNLOCKED.register(new EventConsumer<AnyPinnacleUnlockedEvent>() {
            @Override
            public void accept(AnyPinnacleUnlockedEvent event) {
                // one shared drop for the whole arena - a party carrying an un-unlocked player still
                // gets the fragment, matching how the relic/map drops in this arena are group loot
                event.anyUnlocked = event.players.stream().anyMatch(p -> Load.player(p).atlas.pinnacleUnlocked);
            }
        });

        DungeonExileEvents.GET_RELIC_FIND_BONUS.register(new EventConsumer<GetRelicFindBonusEvent>() {
            @Override
            public void accept(GetRelicFindBonusEvent event) {
                event.bonusPercent = Load.Unit(event.player).getUnit().getCalculatedStat(RelicFind.getInstance()).getValue();
            }
        });

        DungeonExileEvents.GET_DUPLICATE_MAP_CHANCE.register(new EventConsumer<GetDuplicateMapChanceEvent>() {
            @Override
            public void accept(GetDuplicateMapChanceEvent event) {
                event.bonusPercent = Load.Unit(event.player).getUnit().getCalculatedStat(DuplicateMapChance.getInstance()).getValue();
            }
        });

        DungeonExileEvents.GET_MAP_CONTENT_WEIGHT_BONUS.register(new EventConsumer<GetMapContentWeightBonusEvent>() {
            @Override
            public void accept(GetMapContentWeightBonusEvent event) {
                // map the shared MapContent id (see MnsMapContents / ObeliskMapContents / HarvestMapContents)
                // to the matching Atlas "event chance" stat. String literals so we don't compile-depend on
                // the harvest/obelisk mods; if a league mod is absent its content isn't registered anyway.
                Stat stat;
                int minLevel;
                switch (event.contentId) {
                    case "prophecy":
                        stat = ProphecyEventChance.getInstance();
                        minLevel = ServerContainer.get().MIN_LEVEL_PROPHECY_IN_MAPS.get();
                        break;
                    case "obelisk":
                        stat = ObeliskEventChance.getInstance();
                        minLevel = ServerContainer.get().MIN_LEVEL_OBELISK_IN_MAPS.get();
                        break;
                    case "the_harvest":
                        stat = HarvestEventChance.getInstance();
                        minLevel = ServerContainer.get().MIN_LEVEL_HARVEST_IN_MAPS.get();
                        break;
                    case "strongbox":
                        stat = StrongboxEventChance.getInstance();
                        minLevel = ServerContainer.get().MIN_LEVEL_STRONGBOX_IN_MAPS.get();
                        break;
                    case "imprisoned_monster":
                        stat = ImprisonedMonsterEventChance.getInstance();
                        minLevel = ServerContainer.get().MIN_LEVEL_IMPRISONED_MONSTER_IN_MAPS.get();
                        break;
                    case "shrine":
                        stat = ShrineEventChance.getInstance();
                        minLevel = ServerContainer.get().MIN_LEVEL_SHRINE_IN_MAPS.get();
                        break;
                    default:
                        return;
                }

                // below the configured min level, this league mechanic can't spawn in maps at all -
                // regular overworld Harvest/Obelisk blocks and their own maps are untouched by this,
                // it only gates them showing up as bonus content inside a dungeon map run.
                int highestLevel = 0;
                for (Player p : event.players) {
                    int lvl = Load.Unit(p).getLevel();
                    if (lvl > highestLevel) {
                        highestLevel = lvl;
                    }
                }
                if (highestLevel < minLevel) {
                    event.blocked = true;
                    return;
                }

                // per content: (its event-chance stat) - (the shared EventFocusPenalty). A Singular
                // Focus node boosts one event's chance by the same amount as the penalty, so the focused
                // event nets to unpenalized while every other event is reduced. True per-player max (NOT
                // floored at 0) so the negative penalty actually lowers weight. Only ever one player here.
                Float best = null;
                for (Player p : event.players) {
                    float value = Load.Unit(p).getUnit().getCalculatedStat(stat).getValue()
                            - Load.Unit(p).getUnit().getCalculatedStat(EventFocusPenalty.getInstance()).getValue();
                    if (best == null || value > best) {
                        best = value;
                    }
                }
                if (best != null) {
                    event.bonusPercent = best;
                }
            }
        });

        DungeonExileEvents.GET_BONUS_CONTENT_CHANCE.register(new EventConsumer<GetBonusContentChanceEvent>() {
            @Override
            public void accept(GetBonusContentChanceEvent event) {
                float chance = Load.Unit(event.player).getUnit().getCalculatedStat(DoubleEventChance.getInstance()).getValue();

                // map tier feeds the same pool: higher tier maps just have a higher chance at more
                // league mechanics, scaling linearly from nothing at tier 0 to the configured value
                // at max tier. dungeon_realm has no concept of map tiers, which is why this lives here.
                MapItemData map = StackSaving.MAP.loadFrom(event.mapStack);
                if (map != null) {
                    int maxTier = MapItemData.maxMapTier();
                    if (maxTier > 0) {
                        chance += ServerContainer.get().MAX_TIER_BONUS_EVENT_CHANCE.get() * (map.tier / (float) maxTier);
                    }
                }

                event.bonusPercent = chance;
            }
        });

        DungeonExileEvents.ON_MAP_FULLY_CLEARED.register(new EventConsumer<OnMapFullyClearedEvent>() {
            @Override
            public void accept(OnMapFullyClearedEvent event) {
                // vanilla stats-screen tracking - everyone in the map instance gets credit (event.players
                // is every player in this specific instance, not just the killer), same "who was there"
                // semantics as the rest of this event. Pinnacle is the exception: a low-level party member
                // could get carried into someone else's pinnacle map without having unlocked pinnacle
                // themselves, so that one still requires the player's own atlas.pinnacleUnlocked
                for (Player p : event.players) {
                    p.awardStat(Stats.CUSTOM.get(PlayerStats.DUNGEONS_COMPLETED));
                    if (event.uber) {
                        p.awardStat(Stats.CUSTOM.get(PlayerStats.UBER_DUNGEONS_COMPLETED));
                    }
                    if (event.pinnacle && Load.player(p).atlas.pinnacleUnlocked) {
                        p.awardStat(Stats.CUSTOM.get(PlayerStats.PINNACLE_DUNGEONS_COMPLETED));
                    }
                }

                // resolve this specific run's rolled tier/rarity the same way CAN_ENTER_MAP does,
                // so each Atlas node for this dungeon can be gated on its own requirement
                MapData runMap = WorldData.get(event.level).map.getData(DungeonMain.MAIN_DUNGEON_STRUCTURE, event.pos);
                int tier = runMap != null ? runMap.map.tier : 0;
                String rar = runMap != null ? runMap.map.rar : IRarity.COMMON_ID;

                for (AtlasNode node : AtlasNodeUtils.byDungeon(event.dungeonGuid)) {
                    if (!meetsRequirement(node, tier, rar, event.uber)) {
                        continue;
                    }
                    for (Player p : event.players) {
                        PlayerData pd = Load.player(p);
                        // only credit players who'd already unlocked this node themselves -
                        // i.e. who could have targeted/rolled this dungeon on their own atlas,
                        // not just whoever happened to be along for the ride
                        if (!pd.atlas.isUnlocked(node.id)) {
                            continue;
                        }
                        if (pd.atlas.markCompleted(node)) {
                            pd.points.get(PlayerPointsType.ATLAS).giveBonusPoints(node.atlas_points_reward);
                            pd.playerDataSync.setDirty();

                            if (node.is_pinnacle_unlock && !pd.atlas.pinnacleUnlocked) {
                                // only pinnacle nodes actually placed on the atlas map layout count -
                                // the registry holds many generated nodes that aren't on the grid, and
                                // an unplaced one would otherwise make Pinnacle impossible to unlock
                                java.util.Set<String> placed = AtlasNodeLayout.mainCalcData().pointOf.keySet();
                                var placedPinnacle = DungeonDatabase.AtlasNodes().getList().stream()
                                        .filter(n -> n.is_pinnacle_unlock)
                                        .filter(n -> placed.contains(n.id))
                                        .toList();
                                boolean allDone = !placedPinnacle.isEmpty()
                                        && placedPinnacle.stream().allMatch(n -> pd.atlas.isCompleted(n.id));
                                if (allDone) {
                                    pd.atlas.pinnacleUnlocked = true;
                                    pd.playerDataSync.setDirty();
                                    OnScreenMessageUtils.sendMessage((ServerPlayer) p,
                                            Component.literal("Pinnacle Unlocked").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                                            Component.literal("You may now craft a Pinnacle Upgrade").withStyle(ChatFormatting.GRAY));
                                    SoundUtils.playSound(p.level(), p.blockPosition(), SoundEvents.WITHER_SPAWN);
                                }
                            }
                        }
                    }
                }
            }
        });

        DungeonExileEvents.CAN_ENTER_MAP.register(new EventConsumer<CanEnterMapEvent>() {
            @Override
            public void accept(CanEnterMapEvent event) {

                try {
                    // we use this getter here because we don't want to check dimension ids now
                    var map = WorldData.get(event.p.level()).map.getData(DungeonMain.MAIN_DUNGEON_STRUCTURE, event.mapDevice.pos);

                    if (map == null) {
                        event.canEnter = false;
                        return;
                    }
                    if (!canJoinMap(event.p, map)) {
                        event.canEnter = false;
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    static boolean meetsRequirement(AtlasNode node, int tier, String rarityId, boolean uber) {
        if (node.require_uber && !uber) {
            return false;
        }
        if (node.min_tier > 0 && tier < node.min_tier) {
            return false;
        }
        if (!node.min_rarity.isEmpty()) {
            // a node can name a rarity that isn't registered (typo, or a datapack that removed it) -
            // treat an unresolvable rarity as "no rarity requirement" rather than NPEing on the
            // map-completion path, which runs for every player on every cleared map
            var rarity = ExileDB.GearRarities().get(node.min_rarity);
            if (rarity != null && tier < rarity.map_tiers.min) {
                return false;
            }
        }
        return true;
    }

    static boolean checkCooldown(Player p) {

        var cds = Load.Unit(p).getCooldowns();

        if (!p.isCreative()) {
            if (cds.isOnCooldown("start_map")) {
                int sec = cds.getCooldownTicks("start_map") / 20;
                p.sendSystemMessage(ExplainedResultUtil.createErrorAndReason(Chats.MAP_DEVICE_USE_ERROR.locName(), Words.MAP_START_COOLDOWN.locName(sec)));
                return false;
            }
        }
        return true;
    }

    static boolean meetsResists(Player p, MapItemData data) {
        if (!data.getStatReq().meetsReq(Load.Unit(p).getLevel(), Load.Unit(p)) && !p.isCreative()) {
            ExplainedResultUtil.sendErrorMessage(p, Chats.MAP_DEVICE_USE_ERROR, Chats.RESISTS_TOO_LOW_FOR_MAP);

            List<Component> reqDifference = data.getStatReq().getReqDifference(data.lvl, Load.Unit(p));
            if (!reqDifference.isEmpty()) {
                ExplainedResultUtil.sendErrorMessage(p, Chats.MAP_DEVICE_USE_ERROR, Chats.NOT_MEET_MAP_REQ_FIRST_LINE);
                reqDifference.forEach(p::sendSystemMessage);
            }
            return false;
        }
        return true;
    }


    static boolean canJoinMap(Player p, MapData mapData) {


        if (!meetsResists(p, mapData.map)) {
            return false;
        }

        MapItemData map1 = mapData.map;

        if (Load.Unit(p).getLevel() < (map1.lvl - 5)) {
            p.sendSystemMessage(ExplainedResultUtil.createErrorAndReason(Chats.MAP_DEVICE_USE_ERROR, Chats.TOO_LOW_LEVEL));
            return false;
        }

        // Entry Tickets. A courtesy refusal only - the authoritative charge and seal live in
        // MapEntryTickets.onArrival, which is the one place that sees EVERY arrival (a foreign mod's
        // teleport never reaches this event). Checking here just saves the player a pointless round
        // trip into the map and straight back out.
        if (!p.isCreative() && !mapData.hasEntryTicketsLeft()) {
            p.sendSystemMessage(ExplainedResultUtil.createErrorAndReason(Chats.MAP_DEVICE_USE_ERROR, Chats.MAP_OUT_OF_ENTRY_TICKETS));
            return false;
        }

        return true;
    }
}
