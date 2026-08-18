package com.robertx22.mine_and_slash.database.data.item_set;

import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// one tier of an ItemSet: "wear this many pieces, get these stats".
// tiers are cumulative, every tier with pieces <= equipped applies.
public class SetBonus {

    public int pieces = 2;
    public List<StatMod> stats = new ArrayList<>();

    public SetBonus() {

    }

    public SetBonus(int pieces, List<StatMod> stats) {
        this.pieces = pieces;
        this.stats = stats;
    }

    // set bonuses are fixed, not rolled, so they always resolve at 100%. the level is what makes
    // FLAT mods scale (Stat.scale is a no-op for PERCENT/MORE), and it's the average item level of
    // the equipped set pieces.
    public List<ExactStatData> getStats(int lvl) {
        return stats.stream().map(x -> x.ToExactStat(100, lvl)).collect(Collectors.toList());
    }
}
