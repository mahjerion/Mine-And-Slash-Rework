package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.components.PlayerDataCapability;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.EntityFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class TeleportToBossPacket extends MyPacket<TeleportToBossPacket> {

    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("tp_to_boss");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {

    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        ServerPlayer p = (ServerPlayer) ctx.getPlayer();

        if (!(p.level() instanceof ServerLevel level)) {
            return;
        }

        Load.Unit(p).getCooldowns().runIfNoCooldownAndSet("map_teleport", 50, () -> {

            DungeonMain.ifMapData(level, p.blockPosition()).ifPresent(mapData -> {

                if (!mapData.gave_boss_tp) {
                    return;
                }

                if (DungeonMain.ARENA.isInside(level, p.blockPosition())) {
                    p.sendSystemMessage(Chats.ALREADY_IN_ARENA.locName());
                    return;
                }

                if (!EntityFinder.start(p, LivingEntity.class, p.blockPosition()).radius(4).build().isEmpty()) {
                    p.sendSystemMessage(Chats.ENEMY_TOO_CLOSE.locName());
                    return;
                }

                BlockPos pos = mapData.spawnPositions.containsKey(DungeonMain.ARENA.guid())
                        ? BlockPos.of(mapData.spawnPositions.get(DungeonMain.ARENA.guid()))
                        : TeleportUtils.getSpawnTeleportPos(DungeonMain.ARENA, p.blockPosition());

                var dim = level.dimensionTypeId().location();
                PlayerDataCapability.get(p).mapTeleports.teleportToMap(p, dim, dim, pos);
            });
        });
    }

    @Override
    public MyPacket<TeleportToBossPacket> newInstance() {
        return new TeleportToBossPacket();
    }
}
