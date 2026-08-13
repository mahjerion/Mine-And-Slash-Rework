package com.robertx22.mine_and_slash.aoe_data.database.exile_effects;

import com.robertx22.mine_and_slash.aoe_data.database.spells.PartBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.stats.base.EffectCtx;
import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.exile_effects.EffectType;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.exile_effects.VanillaStatData;
import com.robertx22.mine_and_slash.database.data.spells.components.AttachedSpell;
import com.robertx22.mine_and_slash.database.data.spells.components.ComponentPart;
import com.robertx22.mine_and_slash.database.data.spells.components.EntityActivation;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.tags.all.EffectTags;
import com.robertx22.mine_and_slash.tags.imp.EffectTag;
import com.robertx22.mine_and_slash.tags.imp.SpellTag;
import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;

import java.util.ArrayList;

public class ExileEffectBuilder {

    private ExileEffect effect = new ExileEffect();

    public static ExileEffectBuilder of(EffectCtx ctx) {
        ExileEffectBuilder b = new ExileEffectBuilder();
        b.effect.type = ctx.type;
        b.effect.id = ctx.resourcePath;
        b.effect.locName = ctx.locname;

        if (ctx.type == EffectType.beneficial) {
            b.addTags(EffectTags.positive);
        }
        if (ctx.type == EffectType.negative) {
            b.addTags(EffectTags.negative);
        }
        return b;
    }

    public static ExileEffectBuilder food(EffectCtx ctx) {
        ExileEffectBuilder b = of(ctx);
        b.addTags(EffectTags.food);
        b.maxStacks(1);
        return b;
    }

    public ExileEffectBuilder addTags(EffectTag... tags) {
        for (EffectTag tag : tags) {
            if (!effect.tags.contains(tag.GUID())) {
                this.effect.tags.add(tag);
            }
        }
        return this;
    }

    public ExileEffectBuilder addSpellTags(SpellTag... tags) {
        for (SpellTag tag : tags) {
            if (!effect.spell_tags.contains(tag.GUID())) {
                this.effect.spell_tags.add(tag);
            }
        }
        return this;
    }

    public ExileEffectBuilder desc(String desc) {
        this.effect.locdesc = desc;
        return this;
    }

    public ExileEffectBuilder oneOfAKind(String kind) {
        this.effect.one_of_a_kind_id = kind;
        return this;
    }

    public ExileEffectBuilder removeOnSpellCastWithTag(SpellTag tag) {
        this.effect.remove_on_spell_cast = tag;
        return this;
    }

    public ExileEffectBuilder stat(StatMod stat) {
        this.effect.stats.add(stat);
        return this;
    }


    public ExileEffectBuilder vanillaStat(VanillaStatData stat) {
        this.effect.mc_stats.add(stat);
        return this;
    }

    public ExileEffectBuilder maxStacks(int stacks) {
        this.effect.max_stacks = stacks;
        return this;
    }

    public ExileEffectBuilder spell(Spell stat) {
        AttachedSpell incoming = stat.getAttached();

        if (this.effect.spell == null) {
            this.effect.spell = incoming;
            return this;
        }
        // an effect only holds one attached spell, so assigning would silently drop whatever was
        // already added (a commandOnRemove part built before this call). merge instead so call order
        // doesn't matter
        this.effect.spell.on_cast.addAll(incoming.on_cast);
        incoming.entity_components.forEach((en, parts) -> this.effect.spell.entity_components
                .computeIfAbsent(en, x -> new ArrayList<>())
                .addAll(parts));
        return this;
    }

    /**
     * Runs a minecraft command when the effect is removed, for every removal that goes through
     * {@link com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect#onRemove}: the
     * duration running out, the last stack being consumed, remove-on-spell-cast, and respec cleanup.
     * The leading slash is optional. Can be called multiple times to run several commands.
     * <p>
     * The command runs as the <b>caster</b> that applied the effect, not the entity it was on, so
     * {@code @s} and {@code ~ ~ ~} resolve to the applier. Three caveats come from the onRemove hook
     * itself:
     * <p>
     * 1. onRemove only fires the expire spell when the caster can be resolved by uuid in the target's
     * level, so a caster that logged off, died, unloaded or changed dimension means no command. Effects
     * applied without a recorded caster (shrine/stat granted) never run it at all.
     * <p>
     * 2. Two removal paths skip onRemove entirely: dropping effects whose id is no longer registered,
     * and the one_of_a_kind_id sweep in onApply.
     * <p>
     * 3. Commands run at permission level 100 (hardcoded in CommandUtils), so this is op level.
     */
    public ExileEffectBuilder commandOnRemove(String command) {
        ComponentPart part = PartBuilder.justAction(SpellAction.CASTER_USE_COMMAND.create(command));
        part.addActivationRequirement(EntityActivation.ON_EXPIRE);

        getOrCreateAttachedSpell().entity_components
                .computeIfAbsent(Spell.DEFAULT_EN_NAME, x -> new ArrayList<>())
                .add(part);
        return this;
    }

    private AttachedSpell getOrCreateAttachedSpell() {
        if (this.effect.spell == null) {
            this.effect.spell = new AttachedSpell();
        }
        return this.effect.spell;
    }

    public ExileEffectBuilder disableStackingStatBuff() {
        this.effect.stacks_affect_stats = false;
        return this;
    }

    public ExileEffectBuilder stat(float first, float second, Stat stat, ModType type) {
        StatMod data = new StatMod(first, second, stat, type);
        this.effect.stats.add(data);
        return this;
    }

    public ExileEffectBuilder stat(float first, float second, Stat stat) {
        return stat(first, second, stat, ModType.FLAT);
    }

    public ExileEffect build() {
        effect.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        return effect;
    }

}
