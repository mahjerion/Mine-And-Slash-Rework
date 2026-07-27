package com.robertx22.mine_and_slash.loot.league;

import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.mine_and_slash.loot.LootInfo;

import javax.annotation.Nullable;

// Answers "which league does this loot come from?" for a LootInfo whose league can't be worked out
// from its position - an encounter mob, a special boss rarity, anything a future league mechanic
// invents. Register one of these in your mechanic's glue init() and league-locked UniqueGear starts
// dropping from it; nothing in the core loot code needs to know about you.
public interface LootLeagueResolver {

    // higher runs first. Use MOB_TAG for "this specific mob belongs to my encounter"; anything that
    // is more of a fallback should sit below it.
    int MOB_TAG = 100;

    int priority();

    // return null if this resolver doesn't apply - the next one gets a turn, and if none match the
    // position-derived league from LootInfo.setWorld() stands.
    @Nullable
    League resolve(LootInfo info);
}
