package com.robertx22.addons.ancient_obelisks;

import com.robertx22.ancient_obelisks.api.GetObeliskChestBonusEvent;
import com.robertx22.ancient_obelisks.api.ObeliskExileEvents;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ObeliskExtraDrops;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.player.Player;

// Glue package: ancient_obelisks can't see the main mod's player Stat pipeline, so it asks via a
// synchronous ExileEvent query (same pattern DungeonAddonEvents uses for dungeon_realm's Get*Event
// hooks) for the Atlas "Obelisk Extra Drops" bonus that scales ObeliskRewardLogic's reward-chest count.
public class ObeliskAddonEvents {

    public static void init() {

        ObeliskExileEvents.GET_CHEST_REWARD_BONUS.register(new EventConsumer<GetObeliskChestBonusEvent>() {
            @Override
            public void accept(GetObeliskChestBonusEvent event) {
                float max = 0;
                for (Player p : event.players) {
                    float value = Load.Unit(p).getUnit().getCalculatedStat(ObeliskExtraDrops.getInstance()).getValue();
                    if (value > max) {
                        max = value;
                    }
                }
                event.bonusPercent = max;
            }
        });

    }
}
