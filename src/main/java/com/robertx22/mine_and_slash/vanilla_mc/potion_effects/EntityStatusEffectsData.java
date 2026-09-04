package com.robertx22.mine_and_slash.vanilla_mc.potion_effects;

import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffectInstanceData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.SimpleStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.uncommon.effectdatas.ExilePotionEvent;
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
            e.getValue().ticks_left--;
            ExileEffect eff = ExileDB.ExileEffects().get(e.getKey());
            if (eff != null) {
                eff.onTick(en, e.getValue());
            }
        }


        // todo this is probably bit laggy per tick no?
        if (en.tickCount % 80 == 0) {
            // Prevent keeping e.g. auras and stances after respeccing
            // Has to string compare spell UUIDs to look up the new spell level, so it's done infrequently
            exileMap.entrySet().removeIf(x -> {
                if (x.getValue().shouldRemove() || x.getValue().isSpellNoLongerAllocated(en)) {
                    ExileDB.ExileEffects().get(x.getKey()).onRemove(en);
                    return true;
                }
                return false;
            });
        } else {
            exileMap.entrySet().removeIf(x -> {
                if (x.getValue().shouldRemove()) {
                    ExileDB.ExileEffects().get(x.getKey()).onRemove(en);
                    return true;
                }
                return false;
            });
        }

    }

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

    /**
     * Drops every self buff this entity is holding that was granted by the given spell.
     * Used when a skill stops being equipped: otherwise a player casts a long/infinite self buff,
     * swaps that skill out for another one and keeps both.
     * <p>
     * Only self cast buffs are touched, so a buff another player cast on this one survives, and so
     * does anything this spell put on other entities.
     *
     * @return true if anything was removed
     */
    public boolean removeSelfBuffsOfSpell(LivingEntity en, String spellId) {

        if (spellId == null || spellId.isEmpty() || exileMap.isEmpty()) {
            return false;
        }

        // has to go through the effect's own onRemove like the tick loop does, or its vanilla
        // attribute modifiers (mc_stats) stay on the entity forever
        return exileMap.entrySet().removeIf(x -> {
            if (!x.getValue().self_cast || !spellId.equals(x.getValue().spell_id)) {
                return false;
            }
            if (!ExileDB.ExileEffects().isRegistered(x.getKey())) {
                return true;
            }
            ExileDB.ExileEffects().get(x.getKey()).onRemove(en);
            return true;
        });
    }


    public List<ExileEffect> getEffects() {
        return exileMap.keySet().stream().map(x -> ExileDB.ExileEffects().get(x)).collect(Collectors.toList());
    }

    /**
     * Re-reads every active effect's strength multiplier from the caster's and holder's current stats
     * and stores it back into the instance. Called right after a stat calc, so the stats it reads are
     * the ones that just finished.
     *
     * @return true if any multiplier moved, ie the POTION_EFFECT stat context is now stale
     */
    public boolean refreshStrengthMultis(LivingEntity en) {

        if (exileMap.isEmpty()) {
            return false;
        }

        boolean changed = false;

        for (Map.Entry<String, ExileEffectInstanceData> e : exileMap.entrySet()) {
            ExileEffect eff = ExileDB.ExileEffects().get(e.getKey());
            ExileEffectInstanceData inst = e.getValue();
            LivingEntity caster = inst.getCaster(en.level());

            if (eff == null || caster == null) {
                // a buff with no loaded caster already contributes no stats (getExactStats), leave it
                continue;
            }

            float live = ExilePotionEvent.calcCurrentStrengthMulti(caster, en, eff, inst);

            if (Math.abs(live - inst.str_multi) > 0.001F) {
                inst.str_multi = live;
                eff.refreshVanillaStats(en, inst);
                changed = true;
            }
        }

        return changed;
    }

    public StatContext getStats(LivingEntity en) {

        List<ExactStatData> stats = new ArrayList<>();

        for (Map.Entry<String, ExileEffectInstanceData> e : exileMap.entrySet()) {
            ExileEffect eff = ExileDB.ExileEffects().get(e.getKey());
            if (eff != null) {
                var data = e.getValue();
                stats.addAll(eff.getExactStats(e.getValue().getCaster(en.level()), data.getSpellOrNull(), data.stacks, data.str_multi));
            }
        }

        return new SimpleStatCtx(StatContext.StatCtxType.POTION_EFFECT, stats);

    }
}
