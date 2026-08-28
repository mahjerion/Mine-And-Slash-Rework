package com.robertx22.mine_and_slash.database.data.stats.types.resources.magic_shield;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatScaling;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public class MagicShield extends Stat {
    public static String GUID = "magic_shield";

    private MagicShield() {
        this.min = 0;
        this.scaling = StatScaling.NORMAL;
        this.group = StatGroup.MAIN;

        this.order = 0;
        this.icon = "\u2764";
        this.format = ChatFormatting.LIGHT_PURPLE.getName();

    }


    public static MagicShield getInstance() {
        return MagicShield.SingletonHolder.INSTANCE;
    }

    @Override
    public String locDescForLangFile() {
        return "Like health, but works differently and might need different ways to restore.";
    }

    @Override
    public String GUID() {
        return GUID;
    }

    @Override
    public Elements getElement() {
        return null;
    }

    @Override
    public boolean IsPercent() {
        return false;
    }

    @Override
    public String locNameForLangFile() {
        return "Magic Shield";
    }


    // half of chaos damage ignores magic shield and is dealt straight to health. poison is a chaos
    // ailment so it bypasses too. ChaosDoesntBypassMagicShield turns this off for the target.
    public static float CHAOS_BYPASS_PERCENT = 50;

    public static float modifyEntityDamage(DamageEvent effect, DamageEvent.DmgByElement info, float dmg) {


        float current = effect.targetData.getResources().getMagicShield();

        if (current > 0) {

            float bypassing = getChaosDamageBypassingShield(effect, info, dmg);

            float dmgReduced = Mth.clamp(dmg - bypassing, 0, current);

            if (dmgReduced > 0) {

                // self inflicted costs read as a resource cost, not as taking a hit, so they skip
                // the shield sounds the same way they skip the hurt sound
                if (!effect.isPlayerSelfDamage()) {
                    SoundUtils.playSound(effect.target, SoundEvents.GENERIC_HURT, 0.5F, 1);
                    SoundUtils.playSound(effect.target, SoundEvents.GUARDIAN_HURT, 1, 1);
                }

               
                effect.targetData.getResources().spend(effect.target, ResourceType.magic_shield, dmgReduced);


                float finald = dmg - dmgReduced;

                if (finald <= 0) {
                    effect.absorbedCompletely = true;
                }
                return finald;

            }

        }
        return dmg;
    }

    // the shield is applied to the combined damage of every element at once, so the chaos part has to
    // be pulled back out of it. dmg can already be lower than the event's total (mana absorb runs
    // first), so scale by the chaos share of the total instead of using the raw chaos number.
    private static float getChaosDamageBypassingShield(DamageEvent effect, DamageEvent.DmgByElement info, float dmg) {

        if (CHAOS_BYPASS_PERCENT <= 0 || dmg <= 0 || info == null || info.totalDmg <= 0) {
            return 0;
        }

        if (effect.targetData.getUnit()
                .getCalculatedStat(ChaosDoesntBypassMagicShield.getInstance())
                .getValue() > 0) {
            return 0;
        }

        float chaos = info.getDmgmap().getOrDefault(Elements.Shadow, 0F);

        if (chaos <= 0) {
            return 0;
        }

        return dmg * (chaos / info.totalDmg) * (CHAOS_BYPASS_PERCENT / 100F);
    }

    private static class SingletonHolder {
        private static final MagicShield INSTANCE = new MagicShield();
    }
}
