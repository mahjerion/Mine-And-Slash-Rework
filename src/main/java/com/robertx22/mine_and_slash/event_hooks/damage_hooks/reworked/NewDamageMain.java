package com.robertx22.mine_and_slash.event_hooks.damage_hooks.reworked;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.LivingHurtUtils;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.OnNonPlayerDamageEntityEvent;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.OnPlayerDamageEntityEvent;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.util.DmgSourceUtils;
import com.robertx22.mine_and_slash.mixin_ducks.DamageSourceDuck;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.HealthUtils;

public class NewDamageMain {

    public static void init() {

        ExileEvents.DAMAGE_BEFORE_CALC.register(new OnNonPlayerDamageEntityEvent());
        ExileEvents.DAMAGE_BEFORE_CALC.register(new OnPlayerDamageEntityEvent());

        ExileEvents.DAMAGE_BEFORE_CALC.register(new EventConsumer<>() {
            @Override
            public void accept(ExileEvents.OnDamageEntity event) {
                if (event.source != null) {
                    var duck = (DamageSourceDuck) event.source;
                    duck.setOriginalHP(event.mob.getHealth());
                }
            }
        });

        ExileEvents.DAMAGE_AFTER_CALC.register(new EventConsumer<>() {
            @Override
            public int callOrder() {
                return 100;
            }

            @Override
            public void accept(ExileEvents.OnDamageEntity event) {
                if (!CompatConfig.get().DAMAGE_COMPATIBILITY().overridesDamage) {
                    if (event.source != null && !DmgSourceUtils.isMyDmgSource(event.source)) {
                        if (!LivingHurtUtils.isEnviromentalDmg(event.source)) {
                            float vanillaDamage = HealthUtils.realToVanilla(event.mob, event.damage);
                            event.damage = vanillaDamage;
                        }
                    }
                }
            }
        });

        // todo this isnt last sometimes, and other mods might modify it..
        ExileEvents.DAMAGE_AFTER_CALC.register(new EventConsumer<>() {
            @Override
            public int callOrder() {
                return 10000;
            }

            @Override
            public void accept(ExileEvents.OnDamageEntity event) {
                if (event.source != null) {
                    var duck = (DamageSourceDuck) event.source;
                    duck.tryOverrideDmgWithMns(event);
                }
            }
        });
    }
}
