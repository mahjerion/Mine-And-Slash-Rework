package com.robertx22.mine_and_slash.loot.generators.util;

import com.google.common.base.Preconditions;
import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.StatRollQuality;
import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.ExileStacklessData;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.BaseStatsData;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.UniqueStatsData;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GearCreationUtils {

    public static ItemStack CreateStack(ExileStacklessData data, Item item) {

        ItemStack stack = new ItemStack(item);

        var ex = ExileStack.of(stack);
        data.apply(ex);

        return ex.getStack();

    }


    public static ExileStacklessData CreateData(GearBlueprint blueprint) {
        ExileStacklessData data = new ExileStacklessData();

        GearRarity rarity = blueprint.rarity.get();
        GearItemData gear = new GearItemData();

        gear.gtype = blueprint.gearItemSlot.get().GUID();
        gear.lvl = blueprint.level.get();
        gear.rar = rarity.GUID();


        int sockets = rarity.sockets.random();

        for (int i = 0; i < sockets; i++) {
            gear.sockets.addSocket();
        }

        if (rarity.is_unique_item && blueprint.uniquePart.get() != null) {

            UniqueGear unique = blueprint.uniquePart.get();

            Preconditions.checkNotNull(unique);

            gear.rar = ExileDB.GearRarities().get(unique.rarity).GUID();

            gear.gtype = unique.base_gear;
            data.getOrCreate(StackKeys.CUSTOM).data.set(CustomItemData.KEYS.UNIQUE_ID, unique.GUID());
            gear.uniqueStats = new UniqueStatsData();
            gear.uniqueStats.RerollFully(gear);

        } else {
            if (rarity.is_unique_item) {
                // UniqueGearPart found no unique legal for this context (rarity/tier/level/league).
                // Downgrading beats handing out an illegal unique, but it means a player got a common
                // where a unique was rolled - worth a log line if a datapack filtered the pool empty.
                ExileLog.get().warn("Unique rarity rolled but no unique gear was eligible - downgrading to common.");
                gear.rar = IRarity.COMMON_ID;
            }
        }

        gear.baseStats = new BaseStatsData();
        gear.baseStats.RerollFully(gear);
        gear.imp.RerollFully(gear);
        gear.affixes.randomize(gear);

        applyStatRollQuality(blueprint, gear);

        data.set(StackKeys.GEAR, gear);

        return data;
    }

    // Atlas stat_roll_quality biases each rolled affix's percentile toward the top of its range
    // (lerp p -> max by quality%), so dropped gear rolls stronger stats. Only affects the initial
    // drop roll here - crafting/reroll paths that call RerollNumbers directly are untouched.
    private static void applyStatRollQuality(GearBlueprint blueprint, GearItemData gear) {
        if (blueprint.info == null || blueprint.info.playerEntityData == null) {
            return;
        }
        float quality = blueprint.info.playerEntityData.getUnit().getCalculatedStat(StatRollQuality.getInstance()).getValue();
        if (quality <= 0) {
            return;
        }
        float frac = Math.min(1F, quality / 100F);
        for (var affix : gear.affixes.getPrefixesAndSuffixes()) {
            int max = affix.getMinMax().max;
            affix.p = Math.min(max, Math.round(affix.p + (max - affix.p) * frac));
        }
    }
}
