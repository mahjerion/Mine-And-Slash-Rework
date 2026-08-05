package com.robertx22.mine_and_slash.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

// serializeNBT() gets called far more often than we ever call it ourselves. anything that reads an
// entity's nbt makes vanilla rebuild the whole tag - an advancement with an "nbt" predicate on a
// minecraft:tick trigger, or a datapack running "data get entity" - and our capabilities are the
// most expensive part of that tag by a wide margin. on a 25 player server two such readers were
// costing ~14ms of every tick between them, most of it inside our three caps.
//
// so build the tag once and hand out the same instance until something actually changes. callers
// must not mutate what serializeNBT() returns, which is why PlayerData.syncData copies first.
public class CapNbtCache {

    // even if every invalidation path below missed, the cache can only ever be this stale. cheap
    // insurance against a mutation route nobody wired up - one rebuild per cap per 5s per entity.
    private static final long MAX_AGE_TICKS = 100;

    private CompoundTag cached = null;
    private long cachedVersion = Long.MIN_VALUE;
    private long cachedTick = Long.MIN_VALUE;

    private long version = 0;

    // something changed, the next read has to rebuild
    public void markDirty() {
        this.version++;
    }

    // forces a rebuild on the next read no matter what. used before an authoritative save, where
    // handing back a stale tag would write it to disk.
    public void invalidate() {
        this.cached = null;
    }

    public CompoundTag get(Entity entity, Supplier<CompoundTag> builder) {
        return get(entity, 0, builder);
    }

    // extraVersion folds in a counter owned by someone else, so a cap can key its cache off the
    // DirtySync it already maintains instead of a second set of invalidation call sites. both
    // counters only ever increase, so their sum is a valid version too.
    public CompoundTag get(Entity entity, long extraVersion, Supplier<CompoundTag> builder) {

        long now = gameTime(entity);
        long v = this.version + extraVersion;

        boolean freshEnough = now != NO_TIME && now >= cachedTick && now - cachedTick < MAX_AGE_TICKS;

        if (cached != null && cachedVersion == v && freshEnough) {
            return cached;
        }

        this.cached = builder.get();
        this.cachedVersion = v;
        this.cachedTick = now;

        return this.cached;
    }

    private static final long NO_TIME = Long.MIN_VALUE;

    // getGameTime is monotonic, unlike getDayTime which /time set moves around. if we can't read it
    // we rebuild every time, which is just the old behaviour - never serve a tag we can't age.
    private static long gameTime(Entity entity) {
        try {
            if (entity != null && entity.level() != null) {
                return entity.level().getGameTime();
            }
        } catch (Exception e) {
            // fall through
        }
        return NO_TIME;
    }
}
