package com.robertx22.addons.dungeon_realm;

import com.robertx22.dungeon_realm.api.*;
import com.robertx22.dungeon_realm.database.holders.DungeonMapBlocks;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.atlas.AtlasNode;
import com.robertx22.library_of_exile.database.atlas.AtlasNodeUtils;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.vanilla_mc.packets.OpenGuiPacket;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.capability.world.WorldData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.game_balance_config.PlayerPointsType;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.PackSize;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.UberFragmentFind;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.MapBlueprint;
import com.robertx22.mine_and_slash.maps.MapData;
import com.robertx22.mine_and_slash.maps.MapItemData;
import com.robertx22.mine_and_slash.uncommon.ExplainedResultUtil;
import com.robertx22.mine_and_slash.uncommon.UnstuckMobs;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class DungeonAddonEvents {

    public static void init() {

        DungeonExileEvents.ON_GENERATE_NEW_MAP_ITEM.register(new EventConsumer<OnGenerateNewMapItemEvent>() {
            @Override
            public void accept(OnGenerateNewMapItemEvent event) {
                MapBlueprint b = new MapBlueprint(LootInfo.ofLevel(1));
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
                UnstuckMobs.unstuckFromWalls(event.mob);
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

        DungeonExileEvents.GET_UBER_FRAGMENT_FIND_BONUS.register(new EventConsumer<GetUberFragmentFindBonusEvent>() {
            @Override
            public void accept(GetUberFragmentFindBonusEvent event) {
                event.bonusPercent = Load.Unit(event.player).getUnit().getCalculatedStat(UberFragmentFind.getInstance()).getValue();
            }
        });

        DungeonExileEvents.ON_MAP_FULLY_CLEARED.register(new EventConsumer<OnMapFullyClearedEvent>() {
            @Override
            public void accept(OnMapFullyClearedEvent event) {
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
                        }
                    }
                }
            }
        });

        DungeonExileEvents.OPEN_ATLAS_MAP.register(new EventConsumer<OpenAtlasMapEvent>() {
            @Override
            public void accept(OpenAtlasMapEvent event) {
                Packets.sendToClient(event.player, new OpenGuiPacket(OpenGuiPacket.GuiType.ATLAS_MAP));
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
        if (!node.min_rarity.isEmpty() && tier < ExileDB.GearRarities().get(node.min_rarity).map_tiers.min) {
            return false;
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

        return true;
    }
}
