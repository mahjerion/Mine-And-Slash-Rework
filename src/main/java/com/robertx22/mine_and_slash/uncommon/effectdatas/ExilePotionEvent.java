package com.robertx22.mine_and_slash.uncommon.effectdatas;

import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffectInstanceData;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.entities.CalculatedSpellData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class ExilePotionEvent extends EffectEvent {

    public static String ID = "on_exile_effect";

    public String spellid = "";

    @Override
    public String getName() {
        return "MNS Effect Event";
    }

    @Override
    public String GUID() {
        return ID;
    }

    int lvl;

    CalculatedSpellData calc;

    public ExilePotionEvent(CalculatedSpellData calc, int lvl, ExileEffect effect, GiveOrTake2 giveOrTake, LivingEntity caster, LivingEntity target, int tickDuration, boolean infinite) {
        super(1, caster, target);
        this.lvl = lvl;
        this.calc = calc;

        this.data.setupNumber(EventData.STACKS, 1);
        this.data.setString(EventData.GIVE_OR_TAKE, giveOrTake.name());
        this.data.setString(EventData.EXILE_EFFECT, effect.GUID());
        this.data.setupNumber(EventData.EFFECT_DURATION_TICKS, tickDuration);
        this.data.setBoolean(EventData.EFFECT_IS_INFINITE, infinite);
    }

    @Override
    protected void activate() {

        if (source.level().isClientSide) {
            return;
        }
        if (this.data.isCanceled()) {
            return;
        }

        int stacks = (int) data.getNumber(EventData.STACKS).number;

        GiveOrTake2 action = data.getGiveOrTake();
        ExileEffect effect = data.getExileEffect();

        if (action == GiveOrTake2.take) {
            // taking only changes the stack count. the duration and the caster/spell that applied
            // the effect belong to the original application and must survive a partial removal.
            if (!Load.Unit(target).getStatusEffectsData().has(effect)) {
                return;
            }

            ExileEffectInstanceData extraData = Load.Unit(target).getStatusEffectsData().get(effect);

            extraData.stacks -= stacks;
            extraData.stacks = Mth.clamp(extraData.stacks, 0, effect.getMaxCharges(this.targetData));

            // consuming a stack refreshes the shared timer of whatever stacks are left
            if (stacks > 0 && extraData.stacks >= 1 && !extraData.is_infinite && extraData.full_duration > 0) {
                extraData.ticks_left = extraData.full_duration;
            }

            if (extraData.stacks < 1) {
                // the tick loop calls onRemove when it drops an expired effect, so removing it
                // here has to do the same or vanilla attribute modifiers leak
                effect.onRemove(target);
                Load.Unit(target).getStatusEffectsData().delete(effect);
            }

            Load.Unit(target).equipmentCache.STATUS.setDirty();
            return;
        }

        ExileEffectInstanceData extraData = Load.Unit(target).getStatusEffectsData().getOrCreate(effect);

        boolean applied = extraData.stacks == 0;

        extraData.stacks += stacks;
        extraData.stacks = Mth.clamp(extraData.stacks, 1, effect.getMaxCharges(this.targetData));

        // duration and strength always belong to the newest application
        extraData.str_multi = data.getNumber();
        extraData.ticks_left = (int) data.getNumber(EventData.EFFECT_DURATION_TICKS).number;
        extraData.full_duration = extraData.ticks_left;
        extraData.is_infinite = data.getBoolean(EventData.EFFECT_IS_INFINITE);

        // isRegistered rather than isEmpty: the spell registry's empty default is a real spell, so
        // get("") would come back as black hole and read as "this application knows its spell"
        Spell incoming = ExileDB.Spells().isRegistered(this.spellid) ? ExileDB.Spells().get(this.spellid) : null;
        boolean existingHasSpell = ExileDB.Spells().isRegistered(extraData.spell_id);

        // a top up from a stat effect or a shrine carries no spell of its own. it must not erase the
        // spell and caster the buff was cast with: the buff's stats are interpolated by the level of
        // that spell, so it would collapse to its min roll, and its ticking spell would stop passing
        // the spell on to whatever it grants. only a fresh apply, or a re-apply that actually knows
        // its spell, owns the provenance. an unbound top up over an already unbound buff is
        // unchanged, which is every shrine/stat granted effect.
        if (applied || incoming != null || !existingHasSpell) {
            extraData.self_cast = source == target;
            extraData.caster_uuid = source.getStringUUID();
            extraData.spell_id = incoming == null ? "" : this.spellid;

            // a proc (ProcSpellEffect) casts a real spell, but the caster usually never learned it, so
            // the "spell got deallocated" cleanup must not treat the buff it grants as a respec leftover.
            // only buffs applied while the holder actually had the spell allocated stay subject to it
            extraData.ignore_spell_allocation = incoming == null || incoming.getLevelOf(target) < 1;

            extraData.calcSpell = this.calc != CalculatedSpellData.NO_SPELL_RELATED ? this.calc : CalculatedSpellData.blank();
        }

        if (applied) {
            effect.onApply(target);
        }

        Load.Unit(target).equipmentCache.STATUS.setDirty();
    }


}
