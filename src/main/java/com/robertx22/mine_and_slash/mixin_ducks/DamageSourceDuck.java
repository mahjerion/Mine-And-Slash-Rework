package com.robertx22.mine_and_slash.mixin_ducks;

import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.util.AttackInformation;

public interface DamageSourceDuck {

    // Set only by MercenaryKillCreditMixin, on the substitute source it hands to LivingEntity.die:
    // the mercenary that actually landed the kill, whose owner is now the source's causing entity.
    // Null on every damage source that hasn't been through that swap, which is all of them until a
    // mob dies. Lets the on-kill proc branch in CommonEvents tell "the player killed it" apart from
    // "the player's mercenary killed it" now that both report a Player as the causing entity.
    public void setCreditedMerc(MercenaryEntity merc);

    public MercenaryEntity getCreditedMerc();

    public void setMnsDamage(float dmg);

    public float getMnsDamage();

    public void setOriginalHP(float hp);

    public float getOriginalHP();

    public boolean hasMnsDamageOverride();

    public void setOriginalDamage(float hp);

    public float getOriginalDamage();

    default void tryOverrideDmgWithMns(AttackInformation info) {
        if (hasMnsDamageOverride()) {
            info.setAmount(getMnsDamage());

            if (info.getAmount() <= 0) {
                info.setCanceled(true);
            } else {
                info.setCanceled(false);
            }
        }
    }

    default void tryOverrideDmgWithMns(ExileEvents.OnDamageEntity event) {
        if (hasMnsDamageOverride()) {
            event.damage = getMnsDamage();

            if (event.damage <= 0) {
                event.damage = 0;
                event.canceled = true;
            } else {
                event.canceled = false;
            }

        }
    }

}
