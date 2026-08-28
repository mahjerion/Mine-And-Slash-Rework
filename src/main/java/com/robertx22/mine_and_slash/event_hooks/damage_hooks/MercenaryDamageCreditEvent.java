package com.robertx22.mine_and_slash.event_hooks.damage_hooks;

import com.robertx22.library_of_exile.components.EntityInfoComponent;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Credits a mercenary's damage to its owner on the victim's damage scoreboard.
 * <p>
 * {@code EntityDmgStatsData.onDamagedBy} only files a damager under its own uuid when it is a
 * {@link Player} - everything else is lumped into one anonymous {@code enviroOrMobDmg} total, so a
 * mercenary's damage was indistinguishable from lava. {@code OnMobDeathDrops} then resolved the
 * killer from that scoreboard, found no name, and dropped out of the whole loot and experience
 * block: a mercenary that solo killed something awarded nothing, to anyone.
 * <p>
 * Filing the hit under the owner instead is the same claim {@code CommonEvents} already stakes on
 * the vanilla side with {@code setLastHurtByPlayer(owner)}. The library's own consumer still adds
 * this hit to {@code enviroOrMobDmg} as well, which is harmless: that total is only read in the
 * {@code killerEntity == null} guard, and the owner now being on the scoreboard means that guard
 * no longer runs for a mercenary kill.
 */
public class MercenaryDamageCreditEvent extends EventConsumer<ExileEvents.OnDamageEntity> {

    @Override
    public void accept(ExileEvents.OnDamageEntity event) {

        if (event.source == null || event.mob instanceof Player) {
            return;
        }
        if (!(event.damage > 0) || event.damage >= Integer.MAX_VALUE) {
            return;
        }

        // a spell projectile's DamageSource already reports its owner, so this catches casts too
        Entity attacker = event.source.getEntity();

        if (!(attacker instanceof MercenaryEntity merc)) {
            return;
        }
        if (!(merc.getOwner() instanceof ServerPlayer owner)) {
            return;
        }

        EntityInfoComponent.IEntityInfo comp = EntityInfoComponent.get(event.mob);
        if (comp != null) {
            comp.getDamageStats().onDamagedBy(owner, event.damage);
        }
    }

    /** right after the library's own OnMobDamaged, which uses 10 - the damage number is settled by then */
    @Override
    public int callOrder() {
        return 11;
    }
}
