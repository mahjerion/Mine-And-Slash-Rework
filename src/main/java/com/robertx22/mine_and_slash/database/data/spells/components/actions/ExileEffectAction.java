package com.robertx22.mine_and_slash.database.data.spells.components.actions;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.mine_and_slash.aoe_data.database.stats.base.EffectCtx;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EventBuilder;
import com.robertx22.mine_and_slash.uncommon.effectdatas.ExilePotionEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.GiveOrTake2;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.Collection;

import static com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField.*;

public class ExileEffectAction extends SpellAction {

    // todo merge these two or rename..
    public enum GiveOrTake {
        GIVE_STACKS(GiveOrTake2.give),
        REMOVE_STACKS(GiveOrTake2.take),
        REMOVE_NEGATIVE(null);

        private GiveOrTake2 other;

        GiveOrTake(GiveOrTake2 other) {
            this.other = other;
        }

        GiveOrTake2 getOther() {
            return other;
        }
    }

    public static Double INFINITE_DURATION = -1D;

    public ExileEffectAction() {
        super(Arrays.asList(EXILE_POTION_ID, COUNT, POTION_ACTION, POTION_DURATION));
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {

        try {
            String effectId = data.getOrDefault(EXILE_POTION_ID, "");

            ExileEffect potion = data.getExileEffect();
            if (potion == null) {
                // the exile_effect registry has no empty default, so an id that isn't registered
                // (renamed/removed effect, typo in a datapack spell) comes back null and used to
                // NPE deep inside the event with no hint of which spell caused it
                ExileLog.get().warn("Spell action 'exile_effect' has no such exile effect: '" + effectId + "'. Spell: " + getSpellIdForLog(ctx));
                return;
            }

            GiveOrTake action = data.has(POTION_ACTION) ? data.getPotionAction() : GiveOrTake.GIVE_STACKS;
            if (action.getOther() == null) {
                // REMOVE_NEGATIVE only means something for vanilla potions, it has no GiveOrTake2 pair
                ExileLog.get().warn("Spell action 'exile_effect' can't use potion_action: " + action.name() + ". Spell: " + getSpellIdForLog(ctx));
                return;
            }

            int count = data.getOrDefault(COUNT, 1D)
                    .intValue();
            Double durationField = data.getOrDefault(POTION_DURATION, 0D);
            int duration = durationField.intValue();
            boolean infinite = durationField.equals(INFINITE_DURATION);

            float chance = data.getOrDefault(CHANCE, 100D).floatValue();

            // null whenever the spell context isn't tied to a registered spell, ie an effect applied
            // by a stat effect/shrine (CalculatedSpellData.NO_SPELL_RELATED) later ticking its own spell
            Spell ctxSpell = ctx.calculatedSpellData.getSpell();

            // the effect instance still records which spell created it even when the cached
            // CalculatedSpellData it ticks with doesn't, so whatever it grants stays bound
            final Spell spell = ctxSpell != null || ctx.sourceEffect == null ? ctxSpell : ctx.sourceEffect.getSpellOrNull();

            targets.forEach(t -> {

                if (RandomUtils.roll(chance)) {
                    var builder = EventBuilder.ofEffect(ctx.calculatedSpellData, ctx.caster, t, Load.Unit(ctx.caster)
                                    .getLevel(), potion, action.getOther(), duration, infinite)
                            .set(x -> x.data.getNumber(EventData.STACKS).number = count);

                    if (spell != null) {
                        builder.setSpell(spell);
                    }

                    ExilePotionEvent potionEvent = builder.build();

                    if (spell != null) {
                        potionEvent.spellid = spell.GUID();
                    }

                    potionEvent.Activate();
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String getSpellIdForLog(SpellCtx ctx) {
        String id = ctx.calculatedSpellData.spell_id;
        return id == null || id.isEmpty() ? "<none, effect wasn't applied by a spell>" : id;
    }

    public MapHolder giveSeconds(EffectCtx ctx, int seconds) {
        MapHolder dmg = new MapHolder();
        dmg.type = GUID();
        dmg.put(COUNT, 1D);
        dmg.put(POTION_DURATION, seconds * 20D);
        dmg.put(POTION_ACTION, GiveOrTake.GIVE_STACKS.name());
        dmg.put(EXILE_POTION_ID, ctx.resourcePath);
        return dmg;
    }

    public MapHolder create(String id, GiveOrTake action, Double duration) {
        MapHolder dmg = new MapHolder();
        dmg.type = GUID();
        dmg.put(COUNT, 1D);
        dmg.put(POTION_DURATION, duration);
        dmg.put(POTION_ACTION, action.name());
        dmg.put(EXILE_POTION_ID, id);
        return dmg;
    }

    @Override
    public String GUID() {
        return "exile_effect";
    }
}
