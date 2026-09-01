package com.robertx22.mine_and_slash.maps;

import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

public class MapData {

    public MapItemData map = new MapItemData();

    public String playerUuid = "";

    // Entry Tickets spent on this run. Every arrival into the instance costs one - the entry that
    // starts the map, a party member joining, and every return after dying or stepping out.
    //
    // Stored as USED rather than REMAINING on purpose. MapData is Gson-deserialized out of the
    // overworld capability's NBT, so a MapData written before this field existed comes back with 0,
    // which reads as "a fresh, full map" instead of "instantly sealed". Remaining is always derived
    // from the rarity, so a datapack tweak to map_lives also applies to maps already in flight.
    public int entriesUsed = 0;

    // everyone who has already spent a ticket on THIS instance.
    //
    // Exists so a player can step into a league side area rolled inside this map - an Obelisk or
    // Harvest encounter, which live in their OWN dimensions - and walk back in without being charged
    // again, while a teleport in from someone else's run still pays. Keyed per instance rather than
    // per player so it is thrown away with the instance: recycled coordinates get a fresh MapData and
    // therefore a fresh, empty set, and nobody inherits a free pass from the previous occupant.
    //
    // Deliberately NOT stored on PlayerData: that capability is round tripped through NBT on every
    // dimension change (see PlayerData.serializeNBT), which is precisely the event this has to
    // survive, so a transient field there would be wiped exactly when it is needed.
    public Set<String> paidPlayers = new HashSet<>();

    // the size of this map's ticket pool, from its rarity. 0 or less means the rarity opted out and
    // the map can never be sealed.
    public int maxEntryTickets() {
        try {
            return map.getRarity().map_lives;
        } catch (Exception e) {
            // an unresolvable rarity must never seal a map somebody is trying to play
            return 0;
        }
    }

    public int entryTicketsLeft() {
        int max = maxEntryTickets();
        if (max <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, max - entriesUsed);
    }

    public boolean hasEntryTicketsLeft() {
        return entryTicketsLeft() > 0;
    }


    public static MapData newMap(Player p, MapItemData map) {

        Load.player(p).prophecy.affixesTaken.clear();

        MapData data = new MapData();
        data.playerUuid = p.getStringUUID();
        data.map = map;

        return data;

    }


}
