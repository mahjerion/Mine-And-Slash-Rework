package com.robertx22.mine_and_slash.event_hooks.ontick;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.database.data.profession.StationPacket;
import com.robertx22.mine_and_slash.database.data.profession.StationSyncData;
import com.robertx22.mine_and_slash.database.data.profession.screen.CraftingStationMenu;
import com.robertx22.mine_and_slash.database.data.stat_compat.StatCompat;
import com.robertx22.mine_and_slash.gui.screens.map.MapSyncData;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EventBuilder;
import com.robertx22.mine_and_slash.uncommon.effectdatas.RestoreResourceEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.TenSecondPlayerTickEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.RestoreType;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.LevelUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import com.robertx22.mine_and_slash.vanilla_mc.packets.MapCompletePacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class OnServerTick {

    public static AttributeModifier CASTING_SPEED_SLOW = new AttributeModifier(
            UUID.fromString("3fb10485-f309-128f-afc6-a23b0d6cf4c1"),
            BuiltInRegistries.ATTRIBUTE.getKey(Attributes.MOVEMENT_SPEED).toString(),
            -0.5,
            AttributeModifier.Operation.MULTIPLY_TOTAL
    );

    public static void onEndTick(ServerPlayer player) {
        try {
            if (player == null || player.isDeadOrDying()) {
                return;
            }
            EntityData unitdata = Load.Unit(player);
            PlayerData playerData = Load.player(player);


            if (player.level() instanceof ServerLevel sw) {

                WorldUtils.ifMapData(sw, player.blockPosition()).ifPresent(map -> {

                    if (player.tickCount % 20 == 0) {
                        if (map != null && map.map != null) {
                            if (!map.map.getStatReq().meetsReq(map.map.lvl, unitdata)) {
                                //float minusres = map.map.getStatReq().getLackingResistNumber(map.map.lvl, unitdata);
                                player.hurt(player.damageSources().generic(), player.getMaxHealth() * 0.1F);
                            }
                        }
                    }

                    var pro = Load.player(player).prophecy;

                    if (map != null) {
                        if (!pro.mapid.equals(map.map.uuid)) {
                            pro.clearIfNewMap(map.map);
                        }
                    }

                    if (player.tickCount % (20 * 10) == 0) {
                        Packets.sendToClient(player, new MapCompletePacket(new MapSyncData(map)));
                    }

                });


                playerData.spellCastingData.onTimePass(player);
                unitdata.didStatCalcThisTickForPlayer = false;

                int age = player.tickCount;

                if (age % 20 == 0) {
                    StatCompat.onTick(player);
                }
                if (age % 200 == 0) {
                    TenSecondPlayerTickEvent event = new TenSecondPlayerTickEvent(player, player);
                    event.Activate();
                }

                if (age % (20 * 10) == 0) {
                    unitdata.setEquipsChanged();
                }

                if (age % (20 * 3) == 0) {
                    playerData.playerDataSync.setDirty();
                }

                if (age % (20 * 10) == 0) {
                    playerData.miscInfo.area_lvl = LevelUtils.determineLevel(null, player.level(), player.blockPosition(), player, false).getLevel();
                }

                AttributeInstance atri = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (atri != null) {
                    if (playerData.spellCastingData.isCasting() && playerData.spellCastingData.castTickLeft > 0) {
                        if (!atri.hasModifier(CASTING_SPEED_SLOW)) {
                            if (playerData.spellCastingData.getSpellBeingCast() != null && playerData.spellCastingData.getSpellBeingCast().config.slows_when_casting) {
                                atri.addTransientModifier(CASTING_SPEED_SLOW);
                            }
                        }
                    } else {
                        if (atri.hasModifier(CASTING_SPEED_SLOW)) {
                            atri.removeModifier(CASTING_SPEED_SLOW);
                        }
                    }
                }
                if (player.isBlocking()) {
                    if (playerData.spellCastingData.isCasting()) {
                        playerData.spellCastingData.cancelCast(player);
                    }
                }

                if (age % 5 == 0) {
                    var tickrate = 5;

                    unitdata.getResources().onTickBlock(player, tickrate);

                    playerData.spellCastingData.charges.onTicks(player, 5);
                }

                // Every second, apply passive decay to inactive threshold progress
                if (age % 20 == 0) {
                    long now = player.level().getGameTime();
                    var unit = Load.Unit(player);
                    if (unit != null) {
                        // Iterate only active keys per resource
                        for (var rt : ResourceType.values()) {
                            for (var key : unit.getSpendRuntime().getActiveKeys(rt)) {
                                var spec = unit.getSpendRuntime().getSpec(key);
                                long lastAct = unit.getSpendRuntime().getLastActivity(key);
                                if (lastAct <= 0) continue;
                                long since = now - lastAct;
                                if (since < 300) continue; // < 15s

                                long lastDecay = unit.getSpendRuntime().getLastDecay(key);
                                if (lastDecay == now) continue; // already decayed this second
                                unit.getSpendRuntime().markDecay(key, now);

                                // Decay rate: 15% of this threshold's breakpoint per second
                                float thr = spec.thresholdFor(unit);
                                if (thr <= 0f) continue;
                                float decayPerSecond = thr * 0.15f;
                                float newVal = unit.getResourceTracker().decayKeyProgress(key, rt, decayPerSecond);
                                if (newVal <= 0f) {
                                    unit.getSpendRuntime().removeActive(rt, key);
                                }
                            }
                        }
                    }
                }

                if (player.containerMenu instanceof CraftingStationMenu men) {
                    if (player.tickCount % 5 == 0) {
                        men.be.onTickWhenPlayerWatching(player);
                        Packets.sendToClient(player, new StationPacket(new StationSyncData(men.be)));
                    }
                }


                if (age % 20 == 0) {

                    playerData.buff.onTick(player, 20);


                    playerData.favor.onSecond(player);

                    if (unitdata
                            .getResources()
                            .getEnergy() < unitdata
                            .getUnit()
                            .energyData()
                            .getValue() / 10) {
                        if (CompatConfig.get().energyPenalty()) {
                            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 3, 2));
                            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 3, 2));
                        }
                    }


                    if (player.getY() < (double) (player.level().getMinBuildHeight() - 64)) {
                        // if player is in the void bellow the world, stop regen
                    } else {

                        if (!ResourceType.mana.isFull(unitdata)) {
                            RestoreResourceEvent mana = EventBuilder.ofRestore(player, player, ResourceType.mana, RestoreType.regen, 0)
                                    .build();
                            mana.Activate();
                        }

                        if (!player.isBlocking()) {
                            if (!ResourceType.energy.isFull(unitdata)) {
                                RestoreResourceEvent energy = EventBuilder.ofRestore(player, player, ResourceType.energy, RestoreType.regen, 0)
                                        .build();
                                energy.Activate();
                            }
                        } else {
                            if (unitdata.getResources().getEnergy() < 1) {
                                player.getCooldowns().addCooldown(player.getOffhandItem().getItem(), 20 * 3);
                                player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), 20 * 3);
                                player.stopUsingItem();
                            }
                        }

                        if (!ResourceType.magic_shield.isFull(unitdata)) {
                            RestoreResourceEvent msevent = EventBuilder.ofRestore(player, player, ResourceType.magic_shield, RestoreType.regen, 0)
                                    .build();
                            msevent.Activate();
                        }

                        boolean canHeal = player.getFoodData().getFoodLevel() >= 1;

                        if (canHeal) {
                            if (true) { //if (player.getHealth() < player.getMaxHealth()) {
                                RestoreResourceEvent hpevent = EventBuilder.ofRestore(player, player, ResourceType.health, RestoreType.regen, 0)
                                        .build();
                                hpevent.Activate();
                            }
                        }
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Load.player(player).playerDataSync.onTickTrySync(player);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
