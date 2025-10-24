package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Set;


public abstract class SpendThresholdSpec {
    private final ResourceType resource;
    private final float perLevelFactor;
    private final String key;

    private final Set<String> lockWhileEffectIds;
    private final int cooldownTicks;
    private final boolean lockWhileCooldown;         // Used to lock the threshold while the cooldown is active. **RECOMMENDED FOR DEBUGGING ONLY**
    private final boolean dropProgressWhileLocked;
    private final boolean dropProgressOnProc;
    private final boolean showUi;

    private int priority = 0;

    public SpendThresholdSpec(ResourceType resource,
                              float perLevelFactor,
                              String key,
                              Set<String> lockWhileEffectIds,
                              int cooldownTicks,
                              boolean lockWhileCooldown,
                              boolean dropProgressWhileLocked,
                              boolean dropProgressOnProc,
                              boolean showUi) {
        this.resource = resource;
        this.perLevelFactor = perLevelFactor;
        this.key = key;
        this.lockWhileEffectIds = (lockWhileEffectIds == null) ? Collections.emptySet() : Set.copyOf(lockWhileEffectIds);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.lockWhileCooldown = lockWhileCooldown;
        this.dropProgressWhileLocked = dropProgressWhileLocked;
        this.dropProgressOnProc = dropProgressOnProc;
        this.showUi = showUi;
    }

    // ===== accessors =====
    public ResourceType resource()              { return resource; }
    public String key()                         { return key; }
    public String keyFor(EntityData unit)       { return key; }
    public boolean lockWhileCooldown()          { return lockWhileCooldown; }
    public boolean dropProgressWhileLocked()    { return dropProgressWhileLocked; }
    public boolean dropProgressOnProc()         { return dropProgressOnProc; }
    public int cooldownTicks()                  { return cooldownTicks; }
    public int priority()                       { return priority; }
    public boolean showUi()                     { return showUi; }

    
    public SpendThresholdSpec withPriority(int p) {
        this.priority = p;
        return this;
    }

    public SpendThresholdSpec withShowUi(boolean on) {
        return new SpendThresholdSpec(this.resource, this.perLevelFactor, this.key, this.lockWhileEffectIds, this.cooldownTicks, this.lockWhileCooldown, this.dropProgressWhileLocked, this.dropProgressOnProc, on) {
            @Override public float thresholdFor(EntityData unit) { return SpendThresholdSpec.this.thresholdFor(unit); }
            @Override public void onProc(ServerPlayer sp, int procs) { SpendThresholdSpec.this.onProc(sp, procs); }
            @Override public boolean isLockedFor(EntityData unit) { return SpendThresholdSpec.this.isLockedFor(unit); }
        }.withPriority(this.priority);
    }

    public float thresholdFor(EntityData unit) {
        return Math.max(0f, perLevelFactor * Math.max(1, unit.getLevel()));
    }

    public boolean isEffectLocked(EntityData unit) {
        if (lockWhileEffectIds.isEmpty()) return false;
        var store = unit.getStatusEffectsData();
        for (String id : lockWhileEffectIds) {
            ExileEffect effect = ExileDB.ExileEffects().get(id);
            if (effect != null && store.has(effect)) return true;
        }
        return false;
    }

    public boolean isLockedFor(EntityData unit) {
        return isEffectLocked(unit);
    }

    public void startCooldown(EntityData unit, long now) {
        if (cooldownTicks > 0) {
            unit.getSpendRuntime().startCooldown(keyFor(unit), now, cooldownTicks);
        }
    }

    public abstract void onProc(ServerPlayer sp, int procs);

}
