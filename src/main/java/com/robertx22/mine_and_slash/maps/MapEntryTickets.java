package com.robertx22.mine_and_slash.maps;

import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.components.PlayerDataCapability;
import com.robertx22.library_of_exile.dimension.MapDimensions;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.gui.screens.map.MapSyncData;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import com.robertx22.mine_and_slash.vanilla_mc.packets.MapCompletePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Entry Tickets: a map instance can only ever be entered {@code GearRarity.map_lives} times, counted
 * across everyone. Running out seals it - nobody else gets in - but never evicts the players already
 * inside, who keep playing until they leave of their own accord.
 * <p>
 * The whole point of the mechanic is that dying is expensive: vanilla respawns a player at their bed,
 * which is never inside a map dimension, so getting back in is an entry like any other.
 */
public class MapEntryTickets {

    /**
     * Charged from {@code PlayerChangedDimensionEvent}, on ARRIVAL, deliberately.
     * <p>
     * This is the only place that sees every real entry. The mod never calls changeDimension itself -
     * every league entrance funnels through SavedPlayerMapTeleports.teleportToMap, which queues an
     * {@code /execute in <dim> run tp} that DelayedTeleportData fires once the destination chunks are
     * up. That is a vanilla teleport, so it lands here exactly like a foreign mod's compass does, and
     * a teleport that never completes (a disconnect, or TeleportUtils refusing because one was
     * already pending) correctly costs nothing.
     * <p>
     * It is also the authority rather than a second opinion: the map device's CAN_ENTER_MAP check is
     * a courtesy that two players can both pass on the last ticket, and that a foreign teleport never
     * reaches at all. Anyone who arrives with the pool already empty is sent home from here.
     */
    public static void onArrival(ServerPlayer p, ResourceLocation from) {

        try {
            // matches the isCreative() exemptions the other map gates already use
            // (DungeonAddonEvents.checkCooldown / meetsResists), so an admin can inspect a sealed map.
            if (p.isCreative()) {
                return;
            }

            // false = do NOT resolve connected data. With it on, this would follow a side-content
            // instance back to its parent dungeon's MapData and charge that map a ticket for an
            // Obelisk the player walked into. It also does the "is this even the dungeon dimension"
            // check for us - see MapDataFinder.ifMapData.
            var opt = WorldUtils.ifMapData(p.level(), p.blockPosition(), false);
            if (opt.isEmpty()) {
                return;
            }
            MapData map = opt.get();

            // Uber and Pinnacle maps skip the whole mechanic: no counter, no seal, and no
            // "Entry Ticket used" line spammed at the party every time someone walks back in.
            if (map.unlimitedEntries) {
                return;
            }

            // Arriving from another map dimension is how a league side area rolled INSIDE this map
            // gets back - an Obelisk or Harvest encounter lives in its own dimension, so stepping out
            // of one fires this event. That is not a new entry, but only for someone already on this
            // run: the origin dimension alone can't tell those apart, and treating it as enough let a
            // modded teleport hop overworld -> own Obelisk -> a friend's SEALED dungeon and walk in
            // free. Asking whether THIS instance has been paid for answers it exactly.
            if (MapDimensions.isMap(from) && map.paidPlayers.contains(p.getStringUUID())) {
                return;
            }

            if (!map.hasEntryTicketsLeft()) {
                // got here without passing the map device (a foreign teleport), or lost the race for
                // the last ticket against another player arriving in the same tick.
                p.sendSystemMessage(Chats.MAP_OUT_OF_ENTRY_TICKETS.locName().withStyle(ChatFormatting.RED));
                sendHome(p);
                return;
            }

            map.entriesUsed++;
            map.paidPlayers.add(p.getStringUUID());

            int left = map.entryTicketsLeft();
            int max = map.maxEntryTickets();

            // everyone in the instance, not just the arriving player - a party needs to see the pool
            // it shares draining, and the GUI panel reads the same numbers.
            for (Player other : DungeonMain.MAIN_DUNGEON_STRUCTURE.getAllPlayersInMap(p.level(), p.blockPosition())) {
                other.sendSystemMessage(Chats.MAP_ENTRY_TICKET_SPENT.locName(left, max).withStyle(ChatFormatting.GRAY));
                // MapData is otherwise only pushed to clients every 10 seconds from OnServerTick, so
                // resync now rather than letting the Map screen show a stale count for that long.
                Packets.sendToClient(other, new MapCompletePacket(new MapSyncData(map)));
            }

        } catch (Exception e) {
            // a map entry must never be the thing that throws out of a dimension change event
            e.printStackTrace();
        }
    }

    // the capability is null between death and respawn, and this can run on a respawn-driven arrival
    private static void sendHome(ServerPlayer p) {
        try {
            var cap = PlayerDataCapability.get(p);
            if (cap != null && cap.mapTeleports != null) {
                cap.mapTeleports.teleportHome(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
