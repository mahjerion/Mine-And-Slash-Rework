package com.robertx22.addons.ancient_obelisks;

import com.robertx22.ancient_obelisks.api.GetObeliskChestBonusEvent;
import com.robertx22.ancient_obelisks.api.GetObeliskMobToughnessEvent;
import com.robertx22.ancient_obelisks.api.ObeliskExileEvents;
import com.robertx22.ancient_obelisks.capability.ObeliskEntityCapability;
import com.robertx22.ancient_obelisks.database.holders.ObeliskLeagues;
import com.robertx22.library_of_exile.dimension.MapDimensions;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.mine_and_slash.loot.league.LootLeagueResolver;
import com.robertx22.mine_and_slash.loot.league.LootLeagueResolvers;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ObeliskExtraDrops;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ObeliskMobToughness;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.player.Player;

// Glue package: ancient_obelisks can't see the main mod's player Stat pipeline, so it asks via a
// synchronous ExileEvent query (same pattern DungeonAddonEvents uses for dungeon_realm's Get*Event
// hooks) for the Atlas "Obelisk Extra Drops" bonus that scales ObeliskRewardLogic's reward-chest count.
public class ObeliskAddonEvents {

    public static void init() {

        // Obelisk-tagged UniqueGear should drop from obelisk mobs. The obelisk league resolves by
        // position too, but only when the instance was entered as bonus content connected to a dungeon
        // map - a run started from an overworld obelisk has no MapData, so position alone finds nothing.
        // The spawn flag travels with the mob, so it covers that case and mobs that wander out of bounds.
        // The dimension check comes first for allocation, not tidiness: ObeliskEntityCapability.get()
        // ends in .orElse(new ..) and allocates on every miss, and this runs for every mob killed
        // anywhere. Obelisk spawns only ever exist inside a map dimension.
        LootLeagueResolvers.registerMobTag(LootLeagueResolver.MOB_TAG,
                en -> MapDimensions.isMap(en.level()) && ObeliskEntityCapability.get(en).data.isObeSpawn,
                ObeliskLeagues.INSTANCE.OBELISK::get);

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

        ObeliskExileEvents.GET_MOB_TOUGHNESS_BONUS.register(new EventConsumer<GetObeliskMobToughnessEvent>() {
            @Override
            public void accept(GetObeliskMobToughnessEvent event) {
                float max = 0;
                for (Player p : event.players) {
                    float value = Load.Unit(p).getUnit().getCalculatedStat(ObeliskMobToughness.getInstance()).getValue();
                    if (value > max) {
                        max = value;
                    }
                }
                event.bonusPercent = max;
            }
        });

    }
}
