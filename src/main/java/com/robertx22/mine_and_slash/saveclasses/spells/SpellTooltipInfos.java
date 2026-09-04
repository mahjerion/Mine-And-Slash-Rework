package com.robertx22.mine_and_slash.saveclasses.spells;

import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;

// every spell the player has a skill gem for, by spell guid. gson serialized straight into the sync
// packet, so it holds plain fields only.
public class SpellTooltipInfos {

    public HashMap<String, SpellTooltipInfo> map = new HashMap<>();

    public SpellTooltipInfo get(String spellId) {
        return map.get(spellId);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof SpellTooltipInfos that && map.equals(that.map));
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    // null whenever the caster has no gem for this spell, or the entity isn't a player - the tooltip
    // then falls back to the spell's own base numbers
    public static SpellTooltipInfo of(LivingEntity caster, Spell spell) {
        if (!(caster instanceof Player p) || spell == null) {
            return null;
        }
        try {
            return Load.player(p).spellTooltips.get(spell.GUID());
        } catch (Exception e) {
            return null;
        }
    }
}
