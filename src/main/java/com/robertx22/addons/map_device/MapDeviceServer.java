package com.robertx22.addons.map_device;

import com.robertx22.dungeon_realm.block_entity.MapDeviceBE;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.dungeon_realm.item.relic.RelicSlotUtil;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.relic.stat.RelicStatsContainer;
import com.robertx22.library_of_exile.dimension.device.IMapDeviceBlockEntity;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.capability.world.WorldData;
import com.robertx22.mine_and_slash.maps.MapData;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import static com.robertx22.library_of_exile.dimension.device.IMapDeviceBlockEntity.MAP_SLOT;
import static com.robertx22.library_of_exile.dimension.device.IMapDeviceBlockEntity.RELIC_SLOTS;
import static com.robertx22.library_of_exile.dimension.device.IMapDeviceBlockEntity.RELIC_SLOT_START;
import static com.robertx22.library_of_exile.dimension.device.IMapDeviceBlockEntity.SIZE;

/**
 * Server side of the map device screen: resolving the device a packet names, moving items in and out
 * of its slots, starting or joining its run, and sending the client its snapshot. Every entry point
 * re-validates what the client claims - the client only ever picked what to show.
 */
public class MapDeviceServer {

    /** how far a player may stand from the device while using its screen, squared */
    private static final double MAX_REACH_SQ = 8 * 8;

    /**
     * The device at {@code pos}, or null if there is none, the player is in another dimension, or too
     * far away to have clicked it.
     */
    public static IMapDeviceBlockEntity resolve(Player p, BlockPos pos) {
        if (p == null || p.level().isClientSide) {
            return null;
        }
        if (p.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_REACH_SQ) {
            return null;
        }
        BlockEntity be = p.level().getBlockEntity(pos);
        return be instanceof IMapDeviceBlockEntity device ? device : null;
    }

    public static boolean isRelicSlot(int slot) {
        return slot >= RELIC_SLOT_START && slot < RELIC_SLOT_START + RELIC_SLOTS;
    }

    /**
     * Whether {@code stack} may go into {@code slot} of this device. Shared by the client's picker filter
     * and the server's final check, so the two can't drift.
     */
    public static boolean mayPlace(IMapDeviceBlockEntity device, SimpleContainer contents, int slot, ItemStack stack, boolean hasMapSlot) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot == MAP_SLOT) {
            return hasMapSlot && device.acceptsMapItem(stack);
        }
        if (isRelicSlot(slot)) {
            return RelicSlotUtil.canPlace(contents, RELIC_SLOT_START, RELIC_SLOTS, slot, stack);
        }
        return false;
    }

    /**
     * Move one item from the player's inventory slot into a device slot, handing back whatever was there.
     * One item per slot, and the previous stack leaves before the new one lands, so a swap can neither
     * duplicate nor eat anything.
     */
    public static void equip(Player p, IMapDeviceBlockEntity device, BlockPos pos, int invSlot, int slot) {
        if (invSlot < 0 || invSlot >= net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE) {
            return;
        }
        if (slot < 0 || slot >= SIZE) {
            return;
        }
        ItemStack stack = p.getInventory().getItem(invSlot);
        SimpleContainer inv = device.getDeviceInventory();

        if (!mayPlace(device, inv, slot, stack, device.hasMapSlot(p.level()))) {
            return;
        }

        ItemStack previous = inv.getItem(slot);
        ItemStack moving = stack.split(1);

        inv.setItem(slot, moving);
        if (!previous.isEmpty()) {
            giveBack(p, previous);
        }
        markChanged(device);
        sync(p, device, pos);
    }

    public static void clearSlot(Player p, IMapDeviceBlockEntity device, BlockPos pos, int slot) {
        if (slot < 0 || slot >= SIZE) {
            return;
        }
        SimpleContainer inv = device.getDeviceInventory();
        ItemStack stack = inv.getItem(slot);
        if (!stack.isEmpty()) {
            inv.setItem(slot, ItemStack.EMPTY);
            giveBack(p, stack);
            markChanged(device);
        }
        sync(p, device, pos);
    }

    /**
     * The action button. A slotted map (or a waiting free run) starts a new run; otherwise a live run is
     * joined. The client closed its screen when it pressed the button, so the snapshot is only sent back
     * when nothing happened - which reopens the screen for the player to see why (the gate that refused
     * it already sent its own chat message).
     */
    public static void startOrJoin(Player p, IMapDeviceBlockEntity device, BlockPos pos) {
        StartResult result = tryStartOrJoin(p, device);
        if (result != StartResult.STARTED && result != StartResult.JOINED) {
            sync(p, device, pos);
        }
    }

    public enum StartResult {
        STARTED,
        JOINED,
        /** there was something to start or join, but a gate (level, cooldown, tickets...) said no and told the player */
        REFUSED,
        /** no map slotted, no free run waiting, no live run bound */
        NOTHING_TO_DO
    }

    /**
     * What the Initiate/Join button does, shared with the crouch shortcut. A slotted map (or a waiting free
     * run) starts a new run; otherwise a live run is joined.
     */
    public static StartResult tryStartOrJoin(Player p, IMapDeviceBlockEntity device) {
        StartResult result;
        if (device.isFreeRunAvailable(p.level()) || !device.getDeviceInventory().getItem(MAP_SLOT).isEmpty()) {
            SimpleContainer inv = device.getDeviceInventory();
            // resolved by the addon at the exact point the instance is written, never on a refused start
            result = device.startMap(p, () -> consumeRelics(inv)) ? StartResult.STARTED : StartResult.REFUSED;
        } else if (device.isActivated()) {
            result = device.joinMap(p) ? StartResult.JOINED : StartResult.REFUSED;
        } else {
            result = StartResult.NOTHING_TO_DO;
        }
        markChanged(device);
        return result;
    }

    /**
     * The crouch shortcut's map handling: a map held in the main hand that this device accepts goes into
     * the map slot, replacing (and handing back) whatever was there. Anything else in the hand is ignored.
     *
     * @return whether a map was inserted
     */
    public static boolean insertHeldMap(Player p, IMapDeviceBlockEntity device) {
        ItemStack held = p.getMainHandItem();
        SimpleContainer inv = device.getDeviceInventory();
        if (!mayPlace(device, inv, MAP_SLOT, held, device.hasMapSlot(p.level()))) {
            return false;
        }
        ItemStack previous = inv.getItem(MAP_SLOT);
        inv.setItem(MAP_SLOT, held.split(1));
        if (!previous.isEmpty()) {
            giveBack(p, previous);
        }
        markChanged(device);
        return true;
    }

    /** null when nothing is slotted, so the addons can tell "no relics" from "relics with no stats" */
    private static RelicStatsContainer consumeRelics(SimpleContainer inv) {
        boolean any = false;
        for (int i = RELIC_SLOT_START; i < RELIC_SLOT_START + RELIC_SLOTS; i++) {
            if (RelicSlotUtil.isRelic(inv.getItem(i))) {
                any = true;
                break;
            }
        }
        return any ? RelicSlotUtil.consumeAndCalculate(inv, RELIC_SLOT_START, RELIC_SLOTS) : null;
    }

    public static void sync(Player p, IMapDeviceBlockEntity device, BlockPos pos) {
        Packets.sendToClient(p, new MapDeviceSyncPacket(snapshot(p, device, pos)));
    }

    public static MapDeviceClientState snapshot(Player p, IMapDeviceBlockEntity device, BlockPos pos) {
        MapDeviceClientState s = new MapDeviceClientState();
        s.pos = pos;
        s.kind = device.getDeviceKind();
        s.activated = device.isActivated();
        s.hasMapSlot = device.hasMapSlot(p.level());
        s.freeRunAvailable = device.isFreeRunAvailable(p.level());

        SimpleContainer inv = device.getDeviceInventory();
        for (int i = 0; i < SIZE && i < inv.getContainerSize(); i++) {
            s.stacks.set(i, inv.getItem(i).copy());
        }

        if (s.kind.showsEntryTickets) {
            fillTickets(p, device, s);
        }
        return s;
    }

    /**
     * Entry Tickets: for a live run, what the run has left; for a map waiting in the slot, the full pool
     * its rarity grants. Uber and Pinnacle maps are exempt, same as MapData.unlimitedEntries.
     */
    private static void fillTickets(Player p, IMapDeviceBlockEntity device, MapDeviceClientState s) {
        try {
            ItemStack map = s.getMapStack();
            if (!map.isEmpty()) {
                var dungeonItem = DungeonItemNbt.DUNGEON_MAP.loadFrom(map);
                var mapData = StackSaving.MAP.loadFrom(map);
                s.showTickets = true;
                if (dungeonItem != null && (dungeonItem.uber || dungeonItem.pinnacle)) {
                    s.unlimitedTickets = true;
                } else if (mapData != null) {
                    int max = mapData.getRarity().map_lives;
                    if (max <= 0) {
                        s.unlimitedTickets = true;
                    } else {
                        s.ticketsMax = max;
                        s.ticketsLeft = max;
                    }
                }
                return;
            }

            if (s.activated && device instanceof MapDeviceBE be && be.pos != null) {
                MapData run = WorldData.get(p.level()).map.getData(DungeonMain.MAIN_DUNGEON_STRUCTURE, be.pos);
                if (run != null) {
                    s.showTickets = true;
                    if (run.unlimitedEntries || run.maxEntryTickets() <= 0) {
                        s.unlimitedTickets = true;
                    } else {
                        s.ticketsMax = run.maxEntryTickets();
                        s.ticketsLeft = run.entryTicketsLeft();
                    }
                }
            }
        } catch (Exception e) {
            // a map with an unresolvable rarity just shows no ticket line
            s.showTickets = false;
        }
    }

    private static void giveBack(Player p, ItemStack stack) {
        PlayerUtils.forceUnequipItem(stack, p);
    }

    private static void markChanged(IMapDeviceBlockEntity device) {
        device.getDeviceInventory().setChanged();
        if (device instanceof BlockEntity be) {
            be.setChanged();
        }
    }
}
