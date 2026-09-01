package com.robertx22.mine_and_slash.database.data.unique_items.collection;

import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import net.minecraft.util.Mth;

/**
 * Pricing for the unique sticker book.
 * <p>
 * Craft cost and salvage payout run through the SAME factor on purpose. That single property does
 * almost all of the anti exploit work: a rare unique is worth proportionally more to salvage and
 * proportionally more to craft, so there is no item that is cheap to obtain and expensive to buy, and
 * crafting then salvaging the same item always returns the same 8-10% no matter which unique it is.
 */
public class ShardMath {

    // "1000 weight = 1x, 100 weight = 10x", read as a straight line through those two points.
    public static final double WEIGHT_CURVE_INTERCEPT = 11D;
    public static final double WEIGHT_CURVE_DIVISOR = 100D;

    public static final double MIN_WEIGHT_FACTOR = 1D;
    public static final double MAX_WEIGHT_FACTOR = 10.5D;

    /**
     * The rarity multiplier.
     * <p>
     * The clamp is not decoration. Craft to Exile 2 uses a weight of 10000 for league locked uniques
     * (they are the only entry in their own pool, so the number means "always this one", not "common"),
     * and 11 - 10000/100 is -89. Without the floor every league unique would salvage for a negative
     * number of shards.
     */
    public static double weightFactor(UniqueGear unique) {
        double raw = WEIGHT_CURVE_INTERCEPT - (unique.weight / WEIGHT_CURVE_DIVISOR);
        return Mth.clamp(raw, MIN_WEIGHT_FACTOR, MAX_WEIGHT_FACTOR);
    }

    /**
     * weightFactor + drop level tenths + map tier tenths.
     * <p>
     * Additive, not multiplicative. The multiplicative forms break on real data: 237 of CTE2's 307
     * uniques have min_tier 0, so multiplying by tier/10 prices them all at zero, and multiplying by
     * the weight factor makes genuinely rare low level items (tabula_rasa, weight 250, drops at level 1)
     * cost a fifth of a plain mid tier common.
     * <p>
     * min_tier is MAP tier, 0-100. It is not GearItemData.getTier(), which is a 0-5 SkillItemTier.
     */
    public static double valueFactor(UniqueGear unique) {
        return weightFactor(unique)
                + (unique.min_drop_lvl / 10D)
                + (unique.min_tier / 10D);
    }

    /**
     * What it costs to reconstruct this unique. Uses the player's level, because the item comes out at
     * the player's level - price and power stay coupled.
     */
    public static int craftCost(UniqueGear unique, int playerLevel) {
        double base = ServerContainer.get().SHARD_BASE_COST.get();
        return (int) Math.round((base + playerLevel) * valueFactor(unique));
    }

    /**
     * The salvage payout, before the 8-10% roll.
     * <p>
     * Uses the ITEM's level, not the player's. That is what stops low level farming from being
     * worthwhile: a level 10 zone only makes cheap uniques eligible AND stamps them with a low item
     * level, so both halves of this collapse together.
     * <p>
     * The salvaging profession adds a flat percentage to the base constant only (level 50 -> 125,
     * level 100 -> 150). Deliberately not a multiplier on the whole thing, which would double the
     * effective value of every unique in the game for a maxed character.
     */
    public static double salvageValue(UniqueGear unique, int itemLevel, int salvagingProfLevel) {
        double base = ServerContainer.get().SHARD_BASE_COST.get();
        double profBonus = ServerContainer.get().SHARD_SALVAGE_BONUS_PER_PROF_LEVEL.get() * Math.max(0, salvagingProfLevel);
        double scaledBase = base * (1D + profBonus);
        return (scaledBase + itemLevel) * valueFactor(unique);
    }

    /**
     * The actual shard payout. {@code roll} is the caller's random in [0,1), kept as a parameter so
     * the server rolls once per item and nothing here touches a RNG the caller can't see.
     */
    public static int salvageShards(UniqueGear unique, int itemLevel, int salvagingProfLevel, double roll) {
        double min = ServerContainer.get().SHARD_SALVAGE_MIN_PERCENT.get() / 100D;
        double max = ServerContainer.get().SHARD_SALVAGE_MAX_PERCENT.get() / 100D;
        double percent = min + (max - min) * Mth.clamp(roll, 0D, 1D);
        // never pay zero for a real unique, or the cheapest entries read as a bug
        return Math.max(1, (int) Math.floor(salvageValue(unique, itemLevel, salvagingProfLevel) * percent));
    }
}
