package com.robertx22.mine_and_slash.loot;

import com.robertx22.library_of_exile.utils.EntityUtils;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;

public class LootUtils {


    // prevents lvl 50 players farming lvl 1 mobs
    public static float getLevelDistancePunishmentMulti(int level, int playerLevel) {

        if (playerLevel == level) {
            return 1F;
        }

        int num = Math.abs(playerLevel - level);


        int leeway = ServerContainer.get().LEVEL_DISTANCE_PENALTY_LEEWAY.get();

        if (num <= leeway) {
            return 1;
        } else {
            num -= leeway;
        }

        float multi = (float) (1F - num * ServerContainer.get().LEVEL_DISTANCE_PENALTY_PER_LVL.get());

        return (float) Mth.clamp(multi, ServerContainer.get().LEVEL_DISTANCE_PENALTY_MIN_MULTI.get(), 1F);
    }

    private static final float LOOT_HP_REFERENCE = 20F; // a zombie
    private static final float LOOT_HP_DIVISOR = 40F; // with the reference above, pins a zombie at 1.5

    /**
     * Vanilla health stands in for how long a mob takes to kill, but that only holds while players are
     * roughly matched to the content. Once they out-gear it everything dies in one hit, the health stops
     * costing any time, and a linear reward becomes a map-selection exploit - the old {@code 1 + hp/40}
     * paid a 100 hp mob 2.3x what a zombie paid for the same effort, so whichever map's spawn list held
     * the beefiest mobs was simply the best map to farm. The curve is sub-linear so health still nudges
     * reward without deciding which map to run. An exponent of 1, with the cap set back to 10, reproduces
     * the old linear formula exactly.
     * <p>
     * Health comes from the entity type's base attribute, the same source the stat system uses
     * ({@code MobStatUtils.getMobBaseStats}). Live {@code getMaxHealth} would also count foreign
     * MAX_HEALTH attribute modifiers - Ancient Obelisk tier scaling, the library HIGH_HEALTH affix -
     * which grant no Mine and Slash health at all, so those were multiplying loot and exp through a
     * channel nobody intended, obelisk reward climbing with tier all the way into the cap.
     */
    public static float getMobHealthBasedLootMulti(LivingEntity entity) {

        float hp = EntityUtils.getVanillaMaxHealth(entity);

        var rar = Load.Unit(entity).getMobRarity();

        if (rar.forcesCustomHp()) {
            hp = rar.force_custom_hp;
        }
        if (hp < 0) {
            hp = 0;
        }

        float multi = 1F + (LOOT_HP_REFERENCE / LOOT_HP_DIVISOR)
                * (float) Math.pow(hp / LOOT_HP_REFERENCE, ServerContainer.get().MOB_HEALTH_LOOT_EXPONENT.get());

        if (entity instanceof Slime slime) {

            if (ServerContainer.get().MIN_SLIME_SIZE_FOR_LOOT.get() > slime.getSize()) {
                multi = 0;
            } else {
                if (slime.getSize() < 5) {
                    multi *= 0.05F;
                } else {
                    multi *= 0.1F;
                }
            }
        }
        // last, so the slime multi = 0 above survives
        return Math.min(multi, ServerContainer.get().MOB_HEALTH_LOOT_MAX_MULTI.get().floatValue());
    }

    public static int WhileRoll(float chance) {
        int amount = 0;

        while (chance > 0) {

            float maxChance = 75F;

            float currentChance = chance;

            if (currentChance > maxChance) {
                currentChance = maxChance;
            }

            chance -= currentChance;

            if (RandomUtils.roll(currentChance)) {
                amount++;
            }

        }
        return amount;

    }

}
