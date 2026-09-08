package com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.enumclasses.WeaponRange;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import net.minecraft.tags.DamageTypeTags;

public class IsRangedWeapon extends StatCondition {


    public IsRangedWeapon() {
        super("is_ranged_weapon", "is_ranged_weapon");

    }

    @Override
    public boolean can(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {
        var wep = event.data.getWeaponType();
        if (wep.range == WeaponRange.RANGED) {
            return true;
        }
        // a mob's hit is always built with WeaponTypes.none (EntityData.mobBasicAttack), so a
        // skeleton arrow, pillager bolt or blaze fireball read as a melee hit and Projectile Damage
        // Received never applied to them. the vanilla damage source already knows it was a
        // projectile - read that. the bonus element child event carries the same attackInfo, so
        // added flat damage of another element is covered too. not fixed by giving mobs a ranged
        // weapon type: bow hits get scaled by the arrow's draw multiplier, which is only recorded
        // by shootFromRotation, and mobs (and mercenaries) shoot with shoot() - it would be 0.
        if (event instanceof DamageEvent dmg && dmg.attackInfo != null && dmg.attackInfo.getSource() != null) {
            return dmg.attackInfo.getSource().is(DamageTypeTags.IS_PROJECTILE);
        }
        return false;
    }

    @Override
    public Class<? extends StatCondition> getSerClass() {
        return IsRangedWeapon.class;
    }

}

