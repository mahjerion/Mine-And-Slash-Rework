package com.robertx22.mine_and_slash.uncommon.effectdatas.rework.action;

import com.robertx22.mine_and_slash.database.data.spells.components.actions.PositionSource;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;

public class ProcSpellEffect extends StatEffect {

    String spellId = "";
    PositionSource pos = PositionSource.TARGET;

    EffectSides source = EffectSides.Source;
    EffectSides target = EffectSides.Target;

    public boolean use_resource_costs = true;

    // a proc tracks its own cooldown under a key of its own, never the spell's. a proc of a skill the
    // player also has socketed must not lock them out of casting it, grey its hotbar icon, or spend
    // its charges - all of which happened while procs shared the spell's cooldown slot.
    // note this key is not a registered spell id, so tickSpellCooldowns skips it and a cooldown
    // refresh skill cannot wipe a proc guard
    public static String procCooldownKey(String spellId) {
        return "proc_" + spellId;
    }


    public ProcSpellEffect(String spellId, PositionSource pos) {
        super("proc_spell_" + spellId, "proc_spell");
        this.spellId = spellId;
        this.pos = pos;
    }

    ProcSpellEffect() {
        super("", "proc_spell");
    }

    @Override
    public void activate(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {
        var SO = event.getSide(source);
        var TA = event.getSide(target);

        // be careful not to make it proc itself
        var spell = ExileDB.Spells().get(spellId);

        var ctx = new SpellCastContext(SO, 0, spell);

        // a proc triggered by a summon's hit is still the summon's doing. procs are cast from the
        // owner, so this is the only place the summon-ness of the triggering hit can be carried
        // over. chains to any depth, because the proc's own damage re-sets the flag from here.
        ctx.calcData.summon_triggered = event.data.getBoolean(EventData.IS_SUMMON_ATTACK);

        var unit = Load.Unit(SO);
        String cdKey = procCooldownKey(spell.GUID());

        // ALWAYS CHECK THIS BEFORE ACTUALLY CASTING THE PROC SPELL.
        // it also has to come before the resource spend below - a proc that is still on cooldown must
        // not pay for a cast it is about to bail out of
        if (unit.getCooldowns().isOnCooldown(cdKey)) {
            return;
        }

        if (use_resource_costs) {
            if (!unit.getResources().hasEnough(spell.getManaCostCtx(ctx))) {
                return;
            }
            if (!unit.getResources().hasEnough(spell.getEnergyCostCtx(ctx))) {
                return;
            }
            spell.spendResources(ctx);
        }

        // always set the cooldown first, it is the only guard against a proc procing itself. one path
        // for every caster - proc pacing is a property of the spell, not of who triggered it.
        // proc_cooldown_ticks is read raw so no cast speed or cooldown stat can move a proc's rate
        unit.getCooldowns().setOnCooldown(cdKey, spell.config.proc_cooldown_ticks);

        var c = SpellCtx.onCast(SO, ctx.calcData);
        c.setPositionSource(pos);
        c.target = TA;

        // THIS GOES LAST!!! If it's above in might create a stackoverflow
        spell.attached.onCast(c);
    }

    @Override
    public Class<? extends StatEffect> getSerClass() {
        return ProcSpellEffect.class;
    }

}
