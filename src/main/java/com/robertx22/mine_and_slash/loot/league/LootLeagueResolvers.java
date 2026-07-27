package com.robertx22.mine_and_slash.loot.league;

import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.mmorpg.ModErrors;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

// The registry LootInfo consults to attach a league to itself. See LootLeagueResolver.
//
// Priority, highest first:
//   1. explicit assignment - `info.league = x` after the factory call (GearProphecy, StrongboxBlock)
//   2. these resolvers, in priority order
//   3. position - League.getFromPosition() in LootInfo.setWorld()
// (1) wins for free because it happens after setupAllFields() returns.
public class LootLeagueResolvers {

    private static final List<LootLeagueResolver> ALL = new ArrayList<>();

    public static void register(LootLeagueResolver resolver) {
        ALL.add(resolver);
        ALL.sort(Comparator.comparingInt(LootLeagueResolver::priority).reversed());
    }

    // the one-liner shape for "mobs I spawned carry a flag": registerMobTag(MOB_TAG, en -> MyCap.get(en).data.isMine, MyLeagues.INSTANCE.MINE::get)
    public static void registerMobTag(int priority, Predicate<LivingEntity> isTagged, Supplier<League> league) {
        register(new LootLeagueResolver() {
            @Override
            public int priority() {
                return priority;
            }

            @Override
            public League resolve(LootInfo info) {
                if (info.mobKilled == null) {
                    return null;
                }
                return isTagged.test(info.mobKilled) ? league.get() : null;
            }
        });
    }

    @Nullable
    public static League resolve(LootInfo info) {
        for (LootLeagueResolver resolver : ALL) {
            try {
                League league = resolver.resolve(info);
                if (league != null) {
                    return league;
                }
            } catch (Exception e) {
                // one broken resolver must not take down loot generation for everything else
                ModErrors.print(e);
            }
        }
        return null;
    }
}
