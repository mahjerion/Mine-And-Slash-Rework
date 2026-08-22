package com.robertx22.mine_and_slash.database.data.stats.types.resources;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatScaling;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.util.Mth;

public class DamageAbsorbedByMana extends Stat {
    public static String GUID = "mana_shield";

    private DamageAbsorbedByMana() {
        this.min = 0;
        this.scaling = StatScaling.NONE;
        this.group = StatGroup.MAIN;

    }

    public static DamageAbsorbedByMana getInstance() {
        return DamageAbsorbedByMana.SingletonHolder.INSTANCE;
    }

    @Override
    public String locDescForLangFile() {
        return "Percent of damage is absorbed by Mana if remaining Mana is greater than 50 percent of max Mana.";
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
        return true;
    }

    @Override
    public String locNameForLangFile() {
        return "of Damage Absorbed By Mana";
    }


    public static float modifyEntityDamage(DamageEvent effect, float dmg) {

        StatData data = effect.targetData.getUnit()
                .getCalculatedStat(DamageAbsorbedByMana.getInstance());

        if (data.getValue() <= 0) {
            return dmg;
        }

        float currentMana = effect.targetData.getResources()
                .getMana();

        if (currentMana / effect.targetData.getUnit()
                .manaData()
                .getValue() > 0.5F) {

            float maxMana = effect.targetData.getUnit()
                    .manaData()
                    .getValue();

            float dmgReduced = Mth.clamp(dmg * data.getValue() / 100F, 0, currentMana - (maxMana * 0.5F));

            if (dmgReduced > 0) {
                effect.targetData.getResources()
                        .spend(effect.target, ResourceType.mana, dmgReduced);

                return dmg - dmgReduced;

            }

        }
        return dmg;
    }

    private static class SingletonHolder {
        private static final DamageAbsorbedByMana INSTANCE = new DamageAbsorbedByMana();
    }
}
