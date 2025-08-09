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

    public boolean isSpellNoLongerAllocated(LivingEntity en) {
        if (self_cast) {
            //calcSpell.equals(CalculatedSpellData.NO_SPELL_RELATED) indicate this effect is not related to a spell
            if (calcSpell.equals(CalculatedSpellData.NO_SPELL_RELATED)) {
                return false;
            } else {
                Spell spell = getSpell();
                return spell != null && spell.getLevelOf(en) < 1;
            }
        }
        return false;
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
