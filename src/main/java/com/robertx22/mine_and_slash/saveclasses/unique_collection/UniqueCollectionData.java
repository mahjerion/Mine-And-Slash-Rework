package com.robertx22.mine_and_slash.saveclasses.unique_collection;

import java.util.HashSet;
import java.util.Set;

/**
 * The unique "sticker book": which uniques this account has ever salvaged, and how many Philosopher's
 * Shards it holds.
 * <p>
 * Lives directly on PlayerData rather than inside {@link com.robertx22.mine_and_slash.characters.CharacterData},
 * so it is shared by every character on the account - which is the whole point. Switching characters
 * swaps talents, stats and mercs; it must not swap the book.
 * <p>
 * Plain gson POJO, saved through LoadSave. Only gson serializable fields.
 */
public class UniqueCollectionData {

    public Set<String> unlocked = new HashSet<>();
    public int shards = 0;

    public boolean isUnlocked(String uniqueId) {
        return unlocked.contains(uniqueId);
    }

    /**
     * @return true only when this is a NEW unlock, so the caller knows to announce it. Callers must
     * have already checked the entry belongs in the book - league, hidden and retired uniques pay
     * shards but never unlock.
     */
    public boolean unlock(String uniqueId) {
        return unlocked.add(uniqueId);
    }

    public void addShards(int amount) {
        if (amount <= 0) {
            return;
        }
        // clamp rather than wrap. no legitimate income can reach this, but a datapack setting an
        // absurd weight shouldn't be able to flip the balance negative
        long sum = (long) this.shards + amount;
        this.shards = (int) Math.min(sum, Integer.MAX_VALUE);
    }

    /**
     * Floors at 0 rather than going negative - a debt would silently block every future craft with no
     * way for the player to see why.
     */
    public void removeShards(int amount) {
        if (amount <= 0) {
            return;
        }
        this.shards = Math.max(0, this.shards - amount);
    }

    public boolean canAfford(int cost) {
        return cost >= 0 && this.shards >= cost;
    }

    /**
     * Deducts only if affordable. The check and the deduct are one call on purpose - packet handlers
     * run sequentially on the server thread, so this is the whole race protection a double click needs.
     */
    public boolean trySpend(int cost) {
        if (!canAfford(cost)) {
            return false;
        }
        this.shards -= cost;
        return true;
    }
}
