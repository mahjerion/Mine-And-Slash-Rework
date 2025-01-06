package com.robertx22.mine_and_slash.event_hooks.damage_hooks.util;

import com.robertx22.mine_and_slash.mixin_ducks.DamageSourceDuck;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import net.minecraft.world.damagesource.DamageSource;

public class DmgSourceUtils {

    public static boolean isMyDmgSource(DamageSource source) {
        return source.is(DamageEvent.DAMAGE_TYPE) || source.getMsgId().equals(DamageEvent.dmgSourceName);
    }

    public static boolean isMyDmgSourceOrModified(DamageSource source) {
        return isMyDmgSource(source) || (source instanceof DamageSourceDuck d && d.hasMnsDamageOverride());
    }
}
