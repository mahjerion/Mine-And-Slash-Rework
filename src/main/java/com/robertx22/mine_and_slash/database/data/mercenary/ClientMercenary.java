package com.robertx22.mine_and_slash.database.data.mercenary;

import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.ModRange;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRangeInfo;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the local player's mercenary on the client.
 * <p>
 * The client has no {@code MercenaryStorageData.spawnedId} - that field is transient and server side -
 * so the entity has to be located by ownership. That means an AABB entity query, which is far too
 * expensive to run per frame, and both the mercenary screen and the effect overlay want it every
 * frame. So the answer is cached and refreshed once per client tick.
 */
public class ClientMercenary {

    private static MercenaryEntity cached = null;
    private static int cachedOnTick = -1;

    /** the local player's mercenary, or null when it isn't out (or isn't in render range) */
    @Nullable
    public static MercenaryEntity get() {
        Player p = ClientOnly.getPlayer();
        if (p == null) {
            cached = null;
            return null;
        }

        // one search per client tick at most. tickCount also moves when the game is paused in a
        // screen, so a stale entity can't linger for more than a tick either way.
        if (p.tickCount != cachedOnTick) {
            cachedOnTick = p.tickCount;
            cached = search(p);
        }

        // it can die or be dismissed between refreshes, and a world change leaves the old entity
        // behind - checking the level means no disconnect hook is needed to invalidate this
        if (cached != null && (!cached.isAlive() || cached.level() != p.level())) {
            cached = null;
        }
        return cached;
    }

    public static boolean isOut() {
        return get() != null;
    }

    /**
     * Tooltip scoring info for one of the mercenary's own skills. The mercenary is what casts them, so
     * mana cost, cast speed and every {@code [calc:...]} in the description have to be scored off its
     * stats and its weapon rather than off whoever opened the screen.
     * <p>
     * While the mercenary is not out there is no entity to calculate on, so this falls back to the
     * player - callers say so with {@link com.robertx22.mine_and_slash.uncommon.localization.Words#MercenaryNotSummoned}.
     */
    public static StatRangeInfo tooltipInfo() {
        return new StatRangeInfo(ModRange.hide()).setCaster(get());
    }

    @Nullable
    private static MercenaryEntity search(Player p) {
        for (MercenaryEntity en : p.level().getEntitiesOfClass(MercenaryEntity.class, p.getBoundingBox().inflate(48))) {
            if (p.getUUID().equals(en.getOwnerUUID())) {
                return en;
            }
        }
        return null;
    }
}
