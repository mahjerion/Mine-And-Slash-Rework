package com.robertx22.mine_and_slash.event_hooks.damage_hooks.reworked;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.OnNonPlayerDamageEntityEvent;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.OnPlayerDamageEntityEvent;
import com.robertx22.mine_and_slash.mixin_ducks.DamageSourceDuck;

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

                // this is cancelled by the time it hits here, probably..
                /*
                if (event.source != null && event.source.getEntity() instanceof LivingEntity caster) {
                    event.damage = DamageConversion.tryConvert(event.source, caster, event.mob, event.damage);
                }

                 */
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
