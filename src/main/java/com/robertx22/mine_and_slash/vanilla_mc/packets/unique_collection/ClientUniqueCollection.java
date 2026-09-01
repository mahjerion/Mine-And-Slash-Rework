package com.robertx22.mine_and_slash.vanilla_mc.packets.unique_collection;

import java.util.HashSet;
import java.util.Set;

/**
 * The client's mirror of the player's sticker book.
 * <p>
 * Deliberately not part of the PlayerData sync. The unlocked set is ~213 registry guids in Craft to
 * Exile 2 and PlayerData resends its whole tag whenever anything on it changes - profession exp ticks
 * constantly while salvaging - so riding along there would put several KB on the wire for every one of
 * those. This is sent once on login and once per actual change instead.
 */
public class ClientUniqueCollection {

    public static Set<String> unlocked = new HashSet<>();
    public static int shards = 0;

    public static boolean isUnlocked(String id) {
        return unlocked.contains(id);
    }

    public static boolean canAfford(int cost) {
        return shards >= cost;
    }
}
