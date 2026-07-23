package com.robertx22.addons.the_harvest;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects;
import com.robertx22.mine_and_slash.database.data.spells.entities.CalculatedSpellData;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.HarvestCompletionBounty;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.HarvestExtraDrops;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.ExilePotionEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.GiveOrTake2;
import com.robertx22.the_harvest.api.GetHarvestLootBonusEvent;
import com.robertx22.the_harvest.api.HarvestCompletedEvent;
import com.robertx22.the_harvest.api.HarvestExileEvents;
import net.minecraft.world.entity.player.Player;

// Glue package: the_harvest can't see the main mod's player Stat/buff pipeline, so it talks to it via
// synchronous ExileEvents (same pattern DungeonAddonEvents uses for dungeon_realm's Get*Event hooks):
// a query for the Atlas "Harvest Extra Drops" bonus, and a notification when a Harvest completes so
// the Atlas "Bountiful Aftermath" keystone can grant its buff.
public class HarvestAddonEvents {

    public static void init() {

        HarvestExileEvents.GET_HARVEST_LOOT_BONUS.register(new EventConsumer<GetHarvestLootBonusEvent>() {
            @Override
            public void accept(GetHarvestLootBonusEvent event) {
                event.bonusPercent = Load.Unit(event.player).getUnit().getCalculatedStat(HarvestExtraDrops.getInstance()).getValue();
            }
        });

        HarvestExileEvents.HARVEST_COMPLETED.register(new EventConsumer<HarvestCompletedEvent>() {
            @Override
            public void accept(HarvestCompletedEvent event) {
                var effect = ExileDB.ExileEffects().get(ModEffects.HARVEST_BOUNTY.resourcePath);
                if (effect == null) {
                    return;
                }
                for (Player p : event.players) {
                    boolean hasPerk = Load.Unit(p).getUnit().getCalculatedStat(HarvestCompletionBounty.getInstance()).getValue() > 0;
                    if (hasPerk) {
                        new ExilePotionEvent(CalculatedSpellData.NO_SPELL_RELATED, 1, effect, GiveOrTake2.give,
                                p, p, 20 * 60 * 2, false).Activate();
                    }
                }
            }
        });

    }
}
