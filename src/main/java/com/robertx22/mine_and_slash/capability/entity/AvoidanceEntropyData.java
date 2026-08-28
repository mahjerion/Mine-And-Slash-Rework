package com.robertx22.mine_and_slash.capability.entity;

import com.robertx22.library_of_exile.utils.RandomUtils;

import java.util.HashMap;

// PoE style avoidance entropy. instead of rolling dice per hit, every dodge eligible hit charges a counter
// by the dodge chance and the hit is avoided when the counter crosses 100. same long run rate as a plain
// roll, but the dodges land evenly instead of clumping into the streaks that decide whether a character
// lives - which is what made dodge feel like a coin flip you can't build around.
//
// server side scratch state. the field on EntityData is transient on purpose, so this never touches nbt,
// the sync packet or the CapNbtCache. it would reset on unload anyway under the reseed rule below.
public class AvoidanceEntropyData {

    // physical dodge and spell dodge keep separate counters so each stat delivers its own listed rate
    // independently - they don't even share a cap (80 vs 90)
    public static final String DODGE = "dodge";
    public static final String SPELL_DODGE = "spell_dodge";

    // a fresh fight starts from a random point in the cycle, so a player can't count the hits they took
    // and know which swing they're about to dodge. matches PoE reseeding entropy on an idle target.
    public static int RESEED_AFTER_TICKS = 120;

    private static final float FULL = 100F;

    private final HashMap<String, Pool> pools = new HashMap<>();

    private static class Pool {
        // seeded random so two entities don't dodge on the same swing number
        float entropy = RandomUtils.RandomRange(0F, FULL);
        // game time, not entity.tickCount - tickCount resets to 0 on relog and the elapsed maths goes
        // negative, the same trap as reading vanilla's HurtByTimestamp. -1 forces a reseed on first use,
        // which is harmless since the pool is already seeded random.
        long lastCharged = -1;
    }

    public boolean rollAvoid(String id, long gameTime, float chancePercent) {
        if (chancePercent <= 0) {
            return false;
        }
        if (chancePercent >= FULL) {
            return true;
        }

        Pool pool = pools.computeIfAbsent(id, x -> new Pool());

        if (pool.lastCharged < 0 || gameTime - pool.lastCharged > RESEED_AFTER_TICKS) {
            pool.entropy = RandomUtils.RandomRange(0F, FULL);
        }
        pool.lastCharged = gameTime;

        pool.entropy += chancePercent;

        if (pool.entropy >= FULL) {
            pool.entropy -= FULL;
            return true;
        }
        return false;
    }
}
