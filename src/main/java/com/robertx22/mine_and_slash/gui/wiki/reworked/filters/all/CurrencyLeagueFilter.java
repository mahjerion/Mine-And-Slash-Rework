package com.robertx22.mine_and_slash.gui.wiki.reworked.filters.all;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.addon.ExtendedOrb;
import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.mine_and_slash.gui.wiki.BestiaryEntry;
import com.robertx22.mine_and_slash.gui.wiki.reworked.filters.GroupFilterEntry;
import net.minecraft.network.chat.MutableComponent;

public class CurrencyLeagueFilter extends GroupFilterEntry {
    League league;

    public CurrencyLeagueFilter(League league) {
        this.league = league;
    }

    // Only currency actually locked to this league. Previously this passed anything WITHOUT a league
    // requirement too, so every league's button listed nearly the whole currency list and the filter
    // group was noise. Matches how UniqueLeagueFilter behaves, and lets GroupFilterType's
    // getEntriesWithAtLeastOneResult hide the leagues that own no currency at all.
    @Override
    public boolean isValid(BestiaryEntry e) {
        var ext = ExtendedOrb.from(e.obj);
        return ext != null && ext.drop_req.isFromLeague(league);
    }

    @Override
    public MutableComponent getName() {
        return league.getPrettifiedName();
    }
}
