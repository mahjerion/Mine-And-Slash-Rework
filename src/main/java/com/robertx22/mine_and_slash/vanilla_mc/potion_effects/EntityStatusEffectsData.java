package com.robertx22.mine_and_slash.vanilla_mc.potion_effects;

import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffectInstanceData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.SimpleStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.uncommon.effectdatas.ExilePotionEvent;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.Cached;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EntityStatusEffectsData {


    public ConcurrentHashMap<String, ExileEffectInstanceData> exileMap = new ConcurrentHashMap<>();

    // true once this entity has held any exile effect in THIS session. The vanilla modifiers the
    // reconcile below owns are transient (addTransientModifier) and so is this flag, so an entity
    // that has not had an effect since it was loaded provably has none of them and can skip the
    // whole pass. Not serialized - LoadSave is gson based and ignores transient fields.
    private transient boolean everHadEffects = false;

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
        // Prevent keeping e.g. auras and stances after respeccing
        // Has to string compare spell UUIDs to look up the new spell level, so it's done infrequently
        boolean checkAllocation = en.tickCount % 80 == 0;

        removeWhere(en, x -> x.getValue().shouldRemove() || (checkAllocation && x.getValue().isSpellNoLongerAllocated(en)));

    }

    /**
     * Makes the entity's exile effect vanilla attribute modifiers (mc_stats) a pure function of
     * exileMap, in both directions, instead of something that stays correct only if every apply is
     * matched by a remove.
     * <p>
     * Why it has to exist: those modifiers ARE the whole mechanic of the cc effects. Stun is nothing
     * but a x0 on movement speed, attack speed and attack damage - no AI is touched - so one that
     * outlives its effect is a mob rooted in place, or one that swings forever for nothing
     * (EntityData.mobBasicAttack builds mob damage from the vanilla hit amount, which is its
     * ATTACK_DAMAGE attribute). Every removal path calls onRemove today, and StatCalculation.calc
     * strips leftovers, but that net only runs when the entity recalculates its stats, and an
     * exception anywhere in the entity tick can cost an entity both its effect expiry and its
     * recalc. Reconciling from a fixed cadence in its own try/catch is correct no matter which path
     * leaked.
     * <p>
     * The restore half fixes the mirror bug: modifiers are transient, exileMap is saved to nbt, so
     * after a reload a still running stun had its entry, its icon and its timer but no modifiers at
     * all - onApply only fires for the first stack and refreshVanillaStats only when str_multi moves.
     */
    public void reconcileVanillaModifiers(LivingEntity en) {

        if (en.level().isClientSide) {
            return;
        }

        if (!exileMap.isEmpty()) {
            everHadEffects = true;
        }
        if (!everHadEffects) {
            return;
        }

        boolean strippedAny = false;

        for (Cached.ExileEffectVanillaModifier mod : Cached.EXILE_EFFECT_VANILLA_MODIFIERS) {
            AttributeInstance in = en.getAttribute(mod.attribute());

            if (in == null || in.getModifier(mod.uuid()) == null) {
                continue;
            }

            ExileEffectInstanceData inst = exileMap.get(mod.effectId());

            if (inst == null || inst.shouldRemove()) {
                in.removeModifier(mod.uuid());
                strippedAny = true;
                if (MMORPG.RUN_DEV_TOOLS) {
                    System.out.println("[mns] reconcile stripped leaked '" + mod.effectId() + "' modifier on "
                            + BuiltInRegistries.ATTRIBUTE.getKey(mod.attribute()) + " from " + en.getName().getString());
                }
            }
        }

        // restore AFTER the strip, never before. Two effects sharing one modifier uuid is a datapack
        // mistake that has already happened once here (slow used to carry stun's), and in that state
        // the expired one's pass above strips a modifier the live one still needs. Running the
        // restore second means the live effect simply puts it back in the same pass.
        for (Map.Entry<String, ExileEffectInstanceData> e : exileMap.entrySet()) {
            ExileEffectInstanceData inst = e.getValue();

            if (inst.shouldRemove() || !ExileDB.ExileEffects().isRegistered(e.getKey())) {
                continue;
            }

            ExileEffect eff = ExileDB.ExileEffects().get(e.getKey());

            if (eff != null && eff.restoreVanillaStats(en, inst) && MMORPG.RUN_DEV_TOOLS) {
                System.out.println("[mns] reconcile restored missing '" + e.getKey() + "' modifiers on " + en.getName().getString());
            }
        }

        // nothing left to own. re-armed by the next effect that lands
        if (exileMap.isEmpty() && !strippedAny) {
            everHadEffects = false;
        }
    }

    /**
     * Drops every entry matching the filter and runs the effect's onRemove for each. The entry
     * leaves the map BEFORE onRemove runs: onRemove used to be called from inside the removeIf
     * predicate, with the entry still present and its stacks zeroed, so anything the on-expire
     * cast re-applied to this entity (the same effect from a proc, a chained cc) landed on that
     * dying entry, put its vanilla attribute modifiers back through onApply, and was then dropped
     * with the entry - an orphaned x0 attack damage modifier, and a mob that never hurts anyone
     * again. With the entry gone first a re-application creates a fresh, tracked entry.
     *
     * @return true if anything was removed
     */
    private boolean removeWhere(LivingEntity en, java.util.function.Predicate<Map.Entry<String, ExileEffectInstanceData>> filter) {
        List<String> expired = null;
        for (Map.Entry<String, ExileEffectInstanceData> e : exileMap.entrySet()) {
            if (filter.test(e)) {
                if (expired == null) {
                    expired = new ArrayList<>();
                }
                expired.add(e.getKey());
            }
        }
        if (expired == null) {
            return false;
        }
        for (String id : expired) {
            ExileEffectInstanceData removed = exileMap.remove(id);
            ExileEffect eff = ExileDB.ExileEffects().isRegistered(id) ? ExileDB.ExileEffects().get(id) : null;
            if (eff != null) {
                eff.onRemove(en, removed);
            }
        }
        return true;
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
        return removeWhere(en, x -> x.getValue().self_cast && spellId.equals(x.getValue().spell_id));
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

            if (eff == null || inst.shouldRemove()) {
                // an expired entry the tick sweep hasn't dropped yet must not get its vanilla
                // modifiers re-applied - that's how they outlive the effect
                continue;
            }

            LivingEntity caster = inst.getCaster(en.level());

            if (caster == null) {
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
            var data = e.getValue();
            // an expired entry contributes nothing, whether or not the tick sweep got to it yet
            if (eff != null && !data.shouldRemove()) {
                stats.addAll(eff.getExactStats(data.getCaster(en.level()), data.getSpellOrNull(), data.stacks, data.str_multi));
            }
        }

        return new SimpleStatCtx(StatContext.StatCtxType.POTION_EFFECT, stats);

    }
}
