package com.robertx22.mine_and_slash.vanilla_mc.potion_effects;

import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffectInstanceData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.SimpleStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.mmorpg.DebugHud;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EntityStatusEffectsData {


    public ConcurrentHashMap<String, ExileEffectInstanceData> exileMap = new ConcurrentHashMap<>();

    public int getStacks(String eff) {
        if (exileMap.containsKey(eff)) {
            return exileMap.get(eff).stacks;
        }
        return 0;
    }

    public void tick(LivingEntity en) {

        if (exileMap.isEmpty()) {
            return;
        }

        exileMap.entrySet().removeIf(x -> !ExileDB.ExileEffects().isRegistered(x.getKey()));

        for (Map.Entry<String, ExileEffectInstanceData> e : exileMap.entrySet()) {
            ExileEffectInstanceData inst = e.getValue();
            // Clamp to prevent negative countdowns
            if (inst.ticks_left > 0) {
                inst.ticks_left = inst.ticks_left - 1;
            } else if (inst.ticks_left < 0) {
                inst.ticks_left = 0;
            }
            ExileEffect eff = ExileDB.ExileEffects().get(e.getKey());
            if (eff != null) {
                eff.onTick(en, inst);
            }
        }


        // todo this is probably bit laggy per tick no?
        List<ExileEffect> toCallOnRemove = new ArrayList<>();
        List<String> toDeleteKeys = new ArrayList<>();

        if (en.tickCount % 80 == 0) {
            for (Map.Entry<String, ExileEffectInstanceData> entry : exileMap.entrySet()) {
                boolean shouldDrop = entry.getValue().shouldRemove() || entry.getValue().isSpellNoLongerAllocated(en);
                if (shouldDrop) {
                    ExileEffect eff = ExileDB.ExileEffects().get(entry.getKey());
                    if (eff != null) {
                        eff.onRemove(en);
                        ExileEffectInstanceData inst = exileMap.get(entry.getKey());
                        if (inst == null || inst.shouldRemove()) {
                            toDeleteKeys.add(entry.getKey());
                        } else if (DebugHud.ON_EXPIRE && en instanceof net.minecraft.server.level.ServerPlayer sp) {
                            DebugHud.send(sp, "expire_kept_" + entry.getKey(), "[EFFECT][EXPIRE] Kept " + entry.getKey() + " after onRemove (ticks_left=" + inst.ticks_left + ")", 400);
                        }
                    }
                }
            }
        } else {
            for (Map.Entry<String, ExileEffectInstanceData> entry : exileMap.entrySet()) {
                if (entry.getValue().shouldRemove()) {
                    ExileEffect eff = ExileDB.ExileEffects().get(entry.getKey());
                    if (eff != null) {
                        eff.onRemove(en);
                        ExileEffectInstanceData inst = exileMap.get(entry.getKey());
                        if (inst == null || inst.shouldRemove()) {
                            toDeleteKeys.add(entry.getKey());
                        } else if (DebugHud.ON_EXPIRE && en instanceof net.minecraft.server.level.ServerPlayer sp) {
                            DebugHud.send(sp, "expire_kept_" + entry.getKey(), "[EFFECT][EXPIRE] Kept " + entry.getKey() + " after onRemove (ticks_left=" + inst.ticks_left + ")", 400);
                        }
                    }
                }
            }
        }



        for (String key : toDeleteKeys) {
            ExileEffectInstanceData current = exileMap.remove(key);
            if (DebugHud.ON_EXPIRE && en instanceof net.minecraft.server.level.ServerPlayer sp) {
                if (current != null) {
                    DebugHud.send(sp, "expire_removed_" + key, "[EFFECT][EXPIRE] Removed " + key + " (ticks_left=" + current.ticks_left + ", stacks=" + current.stacks + ") [id=" + System.identityHashCode(current) + "]", 200);
                } else {
                    DebugHud.send(sp, "expire_removed_" + key, "[EFFECT][EXPIRE] Removed " + key + " (no current instance)", 200);
                }
            }
        }

    }

    // Removed: processDeferredApplies; no deferral now

    public boolean has(ExileEffect eff) {
        return this.exileMap.containsKey(eff.GUID()) && !exileMap.get(eff.GUID()).shouldRemove();
    }

    public ExileEffectInstanceData get(ExileEffect eff) {
        return exileMap.getOrDefault(eff.GUID(), new ExileEffectInstanceData());
    }

    public ExileEffectInstanceData getOrCreate(ExileEffect eff) {
        if (!exileMap.containsKey(eff.GUID())) {
            exileMap.put(eff.GUID(), new ExileEffectInstanceData());
        }
        return exileMap.getOrDefault(eff.GUID(), new ExileEffectInstanceData());
    }

    public void delete(ExileEffect eff) {
        exileMap.remove(eff.GUID());
    }


    public List<ExileEffect> getEffects() {
        return exileMap.keySet().stream().map(x -> ExileDB.ExileEffects().get(x)).collect(Collectors.toList());
    }

    public StatContext getStats(LivingEntity en) {

        List<ExactStatData> stats = new ArrayList<>();

        for (Map.Entry<String, ExileEffectInstanceData> e : exileMap.entrySet()) {
            ExileEffect eff = ExileDB.ExileEffects().get(e.getKey());
            if (eff != null) {
                var data = e.getValue();
                stats.addAll(eff.getExactStats(e.getValue().getCaster(en.level()), data.getSpell(), data.stacks, data.str_multi));
            }
        }

        return new SimpleStatCtx(StatContext.StatCtxType.POTION_EFFECT, stats);

    }
}
