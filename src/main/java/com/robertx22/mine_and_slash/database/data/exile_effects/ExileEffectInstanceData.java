package com.robertx22.mine_and_slash.database.data.exile_effects;

import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.entities.CalculatedSpellData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.Utilities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.text.DecimalFormat;
import java.util.UUID;

public class ExileEffectInstanceData {

    public CalculatedSpellData calcSpell = CalculatedSpellData.NO_SPELL_RELATED;

    public boolean self_cast = false;
    public boolean is_infinite = false;
    public String caster_uuid = "";
    public String spell_id = "";
    public int stacks = 0;
    public float str_multi = 1;
    public int ticks_left = 0;
    // ticks this effect was applied for. removing a stack refreshes ticks_left back to this
    public int full_duration = 0;
    // set at apply time when the spell that granted this self buff wasn't allocated by the holder.
    // procs (ProcSpellEffect) cast real spells the caster never learned, those buffs have to run out
    // on their own timer instead of being treated as leftovers from a respec
    public boolean ignore_spell_allocation = false;

    public boolean isSpellNoLongerAllocated(LivingEntity en) {
        if (!self_cast || ignore_spell_allocation) {
            return false;
        }
        // effects not tied to a spell (stat/shrine granted) were never allocated to begin with.
        // checked by id instead of calcSpell identity so it survives a relog
        if (spell_id.isEmpty()) {
            return false;
        }
        Spell spell = getSpellOrNull();
        return spell != null && spell.getLevelOf(en) < 1;
    }

    public boolean shouldRemove() {
        return stacks < 1 || (!is_infinite && ticks_left < 1);
    }

    public String getDurationString() {
        if (is_infinite) {
            // Infinity symbol
            return "\u221E";
        }

        int ticks = ticks_left;
        int sec = ticks / 20;
        String text = (int) sec + "s";

        if (sec > 60) {
            int min = sec / 60;
            text = (int) min + "m";
        } else {
            DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");

            if (sec < 10) {
                text = (int) sec + "s";
            } else {
                text = DECIMAL_FORMAT.format(sec / (float) 60) + "m";
            }

        }
        return text;
    }

    public Spell getSpell() {
        return ExileDB.Spells().get(spell_id);
    }

    // the spell registry's empty default is a REAL spell (IntSpells.BLACK_HOLE), so get("") on an
    // effect that isn't tied to a spell silently hands back black hole and everything downstream
    // scales off the holder's level in *that* spell. null is the honest answer, and the callers
    // that interpolate stats already treat a null spell as "don't scale".
    public Spell getSpellOrNull() {
        return ExileDB.Spells().isRegistered(spell_id) ? ExileDB.Spells().get(spell_id) : null;
    }

    public LivingEntity getCaster(Level world) {
        try {
            if (caster_uuid.isEmpty()) {
                return null;
            }
            return Utilities.getLivingEntityByUUID(world, UUID.fromString(caster_uuid));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
