package com.robertx22.mine_and_slash.loot.blueprints.bases;

import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.library_of_exile.registry.FilterListWrap;
import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.mmorpg.ModErrors;

import java.util.ArrayList;

public class UniqueGearPart extends BlueprintPart<UniqueGear, GearBlueprint> {

    public UniqueGearPart(GearBlueprint blueprint) {
        super(blueprint);
        // no legal unique for this loot context is a valid outcome now - see generateIfNull
        this.canBeNull = true;
    }

    // The one place that decides which uniques a caller is allowed to see. Shared with the
    // /give unique_gear "random" command so the two can't drift apart - that command used to roll
    // straight off the registry and could hand out prophecy-only and retired uniques.
    // Pass tier = Integer.MAX_VALUE to opt out of the tier gate (callers with no map context), and
    // LibLeagues.INSTANCE.EMPTY.get() as the league to mean "unrestricted uniques only" -
    // canSpawnInLeague rejects every league-locked unique against it.
    public static FilterListWrap<UniqueGear> eligibleUniques(String rarityId, int tier, int lvl, League league) {
        return ExileDB.UniqueGears()
                .getWrapped()
                .errorIfNothingLeft(false)
                // weight 0 means retired - never hand it out. weightedRandom alone isn't enough:
                // it falls back to a UNIFORM pick when every candidate left is weight 0, and the
                // filters below narrow the pool hard enough for that to happen often. Filtering
                // here rather than at the base_gear step means a slot whose only unique is retired
                // still falls through to a legal unique of another type
                .of(x -> x.Weight() > 0)
                .of(x -> x.rarity.equals(rarityId))
                .of(x -> tier >= x.min_tier)
                .of(x -> lvl >= x.min_drop_lvl)
                .of(x -> x.canSpawnInLeague(league));
    }

    // Weight 0, rarity, min_tier, min_drop_lvl and league are HARD gates: no path here may relax them,
    // or a league-locked unique (a prophecy reward, an uber boss drop) leaks into ordinary loot. Only
    // the base_gear match may be relaxed, because the gear type was rolled at random before we got here
    // and refusing to budge on it would make most uniques undroppable.
    @Override
    protected UniqueGear generateIfNull() {
        try {
            var eligible = eligibleUniques(
                    blueprint.rarity.get().GUID(),
                    blueprint.info.map_tier,
                    blueprint.info.level,
                    blueprint.info.league);

            if (eligible.list.isEmpty()) {
                // nothing legal to give. GearCreationUtils downgrades the item to common rarity, which
                // is the right failure mode - handing back an illegal unique is not.
                return null;
            }

            // prefer one that fits the gear type already rolled, so we don't override it for nothing
            var sameSlot = new FilterListWrap<UniqueGear>(new ArrayList<>(eligible.list))
                    .errorIfNothingLeft(false)
                    .of(x -> x.getBaseGear().GUID().equals(blueprint.gearItemSlot.get().GUID()));

            UniqueGear uniq = sameSlot.list.isEmpty() ? eligible.random() : sameSlot.random();

            if (uniq != null) {
                blueprint.gearItemSlot.override(uniq.getBaseGear());
            }
            return uniq;

        } catch (Exception e) {
            ModErrors.print(e);
            return null;
        }
    }

}
