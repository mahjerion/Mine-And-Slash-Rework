package com.robertx22.addons.map_device;

import com.robertx22.library_of_exile.dimension.MapDimensions;
import com.robertx22.library_of_exile.dimension.device.IMapDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps relics out of the graveyard that a map device inside a map instance otherwise is.
 * <p>
 * The harvest and obelisk blocks the MapContent system places inside a dungeon hand out exactly one free
 * run and then go inert, and the instance they stand in is released from its store half an hour after the
 * last player leaves - its chunks only ever disappear on the boot time dimension wipe. Either way nobody
 * can reach that block again, so a relic still sitting in its slots is gone for good.
 * <p>
 * {@link MapDeviceServer#returnRelics} empties an in-map device the moment its run starts, which covers
 * the case players actually hit. This covers the rest: relics slotted and never used. Every device a
 * player puts a relic into inside a map dimension is remembered here, and handed back when they leave
 * that dimension or respawn out of it.
 * <p>
 * The store is in memory on purpose. Relics that outlive a server restart were already unrecoverable -
 * WIPE_DIMENSION_ON_LOAD deletes the dimension folder, block included, before anything could give them
 * back.
 */
public class MapDeviceRelicGuard {

    /** player -> the in-map devices they have relics sitting in */
    private static final Map<UUID, Set<GlobalPos>> TRACKED = new HashMap<>();

    /** Remember a device the player just put a relic into. No-op outside a map dimension. */
    public static void track(Player p, BlockPos pos) {
        if (p == null || p.level().isClientSide || !MapDimensions.isMap(p.level())) {
            return;
        }
        TRACKED.computeIfAbsent(p.getUUID(), x -> new HashSet<>())
                .add(GlobalPos.of(p.level().dimension(), pos.immutable()));
    }

    /** Drop the device from the player's list once its relic slots are empty again. */
    public static void untrackIfEmpty(Player p, IMapDeviceBlockEntity device, BlockPos pos) {
        if (p == null || device == null || hasAnyRelic(device)) {
            return;
        }
        forget(GlobalPos.of(p.level().dimension(), pos.immutable()));
    }

    /**
     * The player left a map dimension - hand back whatever is still slotted in the devices they used in
     * it. Called after the dimension change, so the player is standing somewhere that keeps a drop.
     */
    public static void onLeftMapDimension(ServerPlayer p, ResourceKey<Level> from) {
        drain(p, from);
    }

    /**
     * Dying inside a map is an exit like any other, and respawning fires no dimension change event, so
     * this leg is what catches it. Every dimension, since the respawn tells us nothing about where the
     * player died.
     */
    public static void onRespawn(ServerPlayer p) {
        drain(p, null);
    }

    /** @param only the dimension to drain, or null for all of them */
    private static void drain(ServerPlayer p, ResourceKey<Level> only) {
        if (p == null || p.getServer() == null) {
            return;
        }
        Set<GlobalPos> tracked = TRACKED.get(p.getUUID());
        if (tracked == null || tracked.isEmpty()) {
            return;
        }

        List<GlobalPos> taking = new ArrayList<>();
        for (GlobalPos gp : tracked) {
            if (only == null || gp.dimension().equals(only)) {
                taking.add(gp);
            }
        }

        for (GlobalPos gp : taking) {
            try {
                returnFrom(p, gp);
            } catch (Exception e) {
                // one unreachable device must not strand the relics in the others
                e.printStackTrace();
            }
            // forgotten for everyone: a second player who had also slotted into this device would
            // otherwise drain a block that no longer holds anything
            forget(gp);
        }
    }

    private static void returnFrom(ServerPlayer p, GlobalPos gp) {
        ServerLevel level = p.getServer().getLevel(gp.dimension());
        if (level == null || !MapDimensions.isMap(level)) {
            return;
        }
        // loads the chunk if it already unloaded behind the player - the block is only ever a few
        // hundred blocks away, in a dimension that is always present
        if (!(level.getBlockEntity(gp.pos()) instanceof IMapDeviceBlockEntity device)) {
            return;
        }
        MapDeviceServer.returnRelics(p, device, false);
    }

    private static void forget(GlobalPos gp) {
        TRACKED.values().forEach(x -> x.remove(gp));
        TRACKED.values().removeIf(Set::isEmpty);
    }

    private static boolean hasAnyRelic(IMapDeviceBlockEntity device) {
        var inv = device.getDeviceInventory();
        for (int i = IMapDeviceBlockEntity.RELIC_SLOT_START;
             i < IMapDeviceBlockEntity.RELIC_SLOT_START + IMapDeviceBlockEntity.RELIC_SLOTS && i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
