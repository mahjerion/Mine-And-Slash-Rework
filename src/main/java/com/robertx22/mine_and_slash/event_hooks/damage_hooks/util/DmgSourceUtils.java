package com.robertx22.mine_and_slash.event_hooks.damage_hooks.util;

import com.robertx22.mine_and_slash.mixin_ducks.DamageSourceDuck;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class DmgSourceUtils {

    public static boolean isMyDmgSource(DamageSource source) {
        return source.is(DamageEvent.DAMAGE_TYPE) || source.getMsgId().equals(DamageEvent.dmgSourceName);
    }

    public static boolean isMyDmgSourceOrModified(DamageSource source) {
        return isMyDmgSource(source) || (source instanceof DamageSourceDuck d && d.hasMnsDamageOverride());
    }

    // a player damaging itself with our own damage shouldn't feel like getting attacked - no hurt
    // tilt, no hurt sound, no knockback. vanilla self damage (own tnt etc) keeps its normal feel.
    public static boolean isPlayerSelfDamage(DamageSource source, LivingEntity target) {
        return target instanceof Player
                && source.getEntity() == target
                && isMyDmgSourceOrModified(source);
    }
}
