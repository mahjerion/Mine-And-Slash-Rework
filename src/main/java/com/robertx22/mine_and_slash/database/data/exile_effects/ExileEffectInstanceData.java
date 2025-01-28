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

    public CalculatedSpellData calcSpell = new CalculatedSpellData(null);

    public String caster_uuid = "";
    public String spell_id = "";
    public int stacks = 0;
    public float str_multi = 1;
    public int ticks_left = 0;


    public boolean shouldRemove() {
        return ticks_left < 1 || stacks < 1;
    }


    public String getDurationString() {
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
