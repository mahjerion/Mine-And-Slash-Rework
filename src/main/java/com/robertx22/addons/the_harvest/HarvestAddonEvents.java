package com.robertx22.addons.the_harvest;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.HarvestExtraDrops;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.the_harvest.api.GetHarvestLootBonusEvent;
import com.robertx22.the_harvest.api.HarvestExileEvents;

// Glue package: the_harvest can't see the main mod's player Stat pipeline, so it asks via a
// synchronous ExileEvent query (same pattern DungeonAddonEvents uses for dungeon_realm's Get*Event
// hooks) for the Atlas "Harvest Extra Drops" bonus that scales the per-mob Harvest loot trigger chance.
public class HarvestAddonEvents {

    public static void init() {

        HarvestExileEvents.GET_HARVEST_LOOT_BONUS.register(new EventConsumer<GetHarvestLootBonusEvent>() {
            @Override
            public void accept(GetHarvestLootBonusEvent event) {
                event.bonusPercent = Load.Unit(event.player).getUnit().getCalculatedStat(HarvestExtraDrops.getInstance()).getValue();
            }
        });

    }
}
