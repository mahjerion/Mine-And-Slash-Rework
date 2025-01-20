package com.robertx22.mine_and_slash.config.forge.compat;

import com.robertx22.mine_and_slash.database.data.stats.types.offense.WeaponDamage;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.LivingHurtUtils;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.util.AttackInformation;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.util.DmgSourceUtils;
import com.robertx22.mine_and_slash.mixin_ducks.DamageSourceDuck;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EventBuilder;
import com.robertx22.mine_and_slash.uncommon.enumclasses.AttackType;
import com.robertx22.mine_and_slash.uncommon.enumclasses.PlayStyle;
import com.robertx22.mine_and_slash.uncommon.enumclasses.WeaponTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class DamageConversion {

    public static float tryConvert(AttackInformation info, DamageSource source, LivingEntity caster, LivingEntity target, float vanillaDmg) {


        if (DmgSourceUtils.isMyDmgSourceOrModified(source)) {
            return vanillaDmg;
        }

        if (LivingHurtUtils.isEnviromentalDmg(source)) {
            return vanillaDmg;
        }

        float dmg = vanillaDmg;

        if (dmg <= 0) {
            if (source instanceof DamageSourceDuck duck) {
                dmg = duck.getOriginalDamage();
            }
        }
        if (dmg <= 0) {
            return vanillaDmg;
        }


        var compat = CompatConfig.get();

        if (compat.vanillaToWepDmgPercent() > 0) {

            float vanillaMulti = 1 - (compat.dmgConversionLoss() / 100F);

            float wepdmg = (float) (dmg * compat.vanillaToWepDmgPercent() / 100F);

            var unit = Load.Unit(caster);

            int num = (int) unit
                    .getUnit()
                    .getCalculatedStat(WeaponDamage.getInstance())
                    .getValue();


            num *= wepdmg;

            vanillaDmg *= vanillaMulti;

            DamageEvent dmgEvent = EventBuilder.ofDamage(caster, target, num)
                    .setupDamage(AttackType.hit, WeaponTypes.none, PlayStyle.STR)
                    .setIsBasicAttack()
                    .build();

            if (compat.dmgConversionLoss() >= 100) {
                dmgEvent.attackInfo = info;
            }

            dmgEvent.Activate();


        } else {
            return 0;
        }
        return vanillaDmg;
    }
}
