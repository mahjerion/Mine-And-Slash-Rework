package com.robertx22.mine_and_slash.mechanics.thresholds;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class SpendThresholdSpec {
    private final ResourceType resource;
    private final float perLevelFactor;  // used by default thresholdFor()
    private final String key;

    // gating/cooldown controls
    private final Set<String> lockWhileEffectIds;
    private final int cooldownTicks;
    private final boolean lockWhileCooldown;         // treat cooldown as a lock
    private final boolean dropProgressWhileLocked;
    private final boolean resetProgressOnProc;
    private final boolean showUi; // whether to render progress HUD for this spec

    // registry ordering (lower runs first)
    private int priority = 0;

    // Full ctor used by data-driven impl
    public SpendThresholdSpec(ResourceType resource,
                              float perLevelFactor,
                              String key,
                              Set<String> lockWhileEffectIds,
                              int cooldownTicks,
                              boolean lockWhileCooldown,
                              boolean dropProgressWhileLocked,
                              boolean resetProgressOnProc,
                              boolean showUi) {
        this.resource = resource;
        this.perLevelFactor = perLevelFactor;
        this.key = key;
        this.lockWhileEffectIds = (lockWhileEffectIds == null) ? Collections.emptySet() : Set.copyOf(lockWhileEffectIds);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.lockWhileCooldown = lockWhileCooldown;
        this.dropProgressWhileLocked = dropProgressWhileLocked;
        this.resetProgressOnProc = resetProgressOnProc;
        this.showUi = showUi;
    }

    // Backward-compatible ctor (defaults showUi=false)
    public SpendThresholdSpec(ResourceType resource,
                              float perLevelFactor,
                              String key,
                              Set<String> lockWhileEffectIds,
                              int cooldownTicks,
                              boolean lockWhileCooldown,
                              boolean dropProgressWhileLocked,
                              boolean resetProgressOnProc) {
        this(resource, perLevelFactor, key, lockWhileEffectIds, cooldownTicks, lockWhileCooldown, dropProgressWhileLocked, resetProgressOnProc, false);
    }

    // ===== accessors =====
    public ResourceType resource()              { return resource; }
    public String key()                         { return key; }
    public String keyFor(EntityData unit)       { return key; }
    public boolean lockWhileCooldown()          { return lockWhileCooldown; }
    public boolean dropProgressWhileLocked()    { return dropProgressWhileLocked; }
    public boolean resetOnProc()                { return resetProgressOnProc; }
    public int cooldownTicks()                  { return cooldownTicks; }
    public int priority()                       { return priority; }
    public boolean showUi()                     { return showUi; }

    // fluent config (for code-defined specs)
    public SpendThresholdSpec withCooldownTicks(int ticks) {
        return newWrapper(this.lockWhileEffectIds, Math.max(0, ticks), this.lockWhileCooldown, this.dropProgressWhileLocked, this.resetProgressOnProc);
    }
    public SpendThresholdSpec lockWhile(String... effectIds) {
        Set<String> s = new HashSet<>(this.lockWhileEffectIds);
        if (effectIds != null) Collections.addAll(s, effectIds);
        return newWrapper(s, this.cooldownTicks, this.lockWhileCooldown, this.dropProgressWhileLocked, this.resetProgressOnProc);
    }
    public SpendThresholdSpec lockWhileCooldown(boolean on) {
        return newWrapper(this.lockWhileEffectIds, this.cooldownTicks, on, this.dropProgressWhileLocked, this.resetProgressOnProc);
    }
    public SpendThresholdSpec dropProgressLocked(boolean on) {
        return newWrapper(this.lockWhileEffectIds, this.cooldownTicks, this.lockWhileCooldown, on, this.resetProgressOnProc);
    }
    public SpendThresholdSpec resetOnProc(boolean on) {
        return newWrapper(this.lockWhileEffectIds, this.cooldownTicks, this.lockWhileCooldown, this.dropProgressWhileLocked, on);
    }
    public SpendThresholdSpec withPriority(int p) {
        this.priority = p;
        return this;
    }

    private SpendThresholdSpec newWrapper(Set<String> lockIds, int cooldown, boolean lockCD, boolean dropLocked, boolean resetOnProc) {
        // create a shallow “copy” retaining dynamic behavior (onProc/thresholdFor come from subclass)
        return new SpendThresholdSpec(this.resource, this.perLevelFactor, this.key, lockIds, cooldown, lockCD, dropLocked, resetOnProc, this.showUi) {
            @Override public float thresholdFor(EntityData unit) { return SpendThresholdSpec.this.thresholdFor(unit); }
            @Override public void onProc(ServerPlayer sp, int procs) { SpendThresholdSpec.this.onProc(sp, procs); }
            @Override public boolean isLockedFor(EntityData unit) { return SpendThresholdSpec.this.isLockedFor(unit); }
        }.withPriority(this.priority);
    }

    public SpendThresholdSpec withShowUi(boolean on) {
        return new SpendThresholdSpec(this.resource, this.perLevelFactor, this.key, this.lockWhileEffectIds, this.cooldownTicks, this.lockWhileCooldown, this.dropProgressWhileLocked, this.resetProgressOnProc, on) {
            @Override public float thresholdFor(EntityData unit) { return SpendThresholdSpec.this.thresholdFor(unit); }
            @Override public void onProc(ServerPlayer sp, int procs) { SpendThresholdSpec.this.onProc(sp, procs); }
            @Override public boolean isLockedFor(EntityData unit) { return SpendThresholdSpec.this.isLockedFor(unit); }
        }.withPriority(this.priority);
    }

    /** Default threshold = perLevelFactor × LVL. Subclasses may override. */
    public float thresholdFor(EntityData unit) {
        return Math.max(0f, perLevelFactor * Math.max(1, unit.getLevel()));
    }

    /** True if any gating effect is active. */
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

    /** Start cooldown (no-op if cooldownTicks == 0). */
    public void startCooldown(EntityData unit, long now) {
        if (cooldownTicks > 0) {
            unit.getSpendRuntime().startCooldown(keyFor(unit), now, cooldownTicks);
        }
    }

    /** Called when one or more thresholds are consumed. */
    public abstract void onProc(ServerPlayer sp, int procs);

}
