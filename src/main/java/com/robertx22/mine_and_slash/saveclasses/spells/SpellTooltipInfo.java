package com.robertx22.mine_and_slash.saveclasses.spells;

import java.util.HashMap;
import java.util.Objects;

// what one spell's tooltip shows for the numbers only the server can work out. the client has the
// synced main unit but never the per spell stat unit (support gems live in it) and never runs the
// stat effect sweep, so on its own it can only show a spell's raw base numbers. this is calculated
// server side by SpellTooltipCalculation and synced whenever the player's stats are recalculated.
public class SpellTooltipInfo {

    // value calculation guid -> what one hit of it deals after every increase, more multiplier and
    // reduction the caster has. the target's mitigation is deliberately not in here
    public HashMap<String, Integer> dmg = new HashMap<>();

    public int mana = 0;
    public int energy = 0;
    public int cooldown_ticks = 0;
    public int charge_cooldown_ticks = 0;
    public int cast_ticks = 0;
    public int recovery_ticks = 0;

    // the spell's own crit chance and crit damage, support gems included. shown as their own lines
    // rather than folded into the damage - the damage above is the plain non critical hit
    public float crit_chance = 0;
    public float crit_damage = 0;

    public Integer getDamage(String valueCalcId) {
        return dmg.get(valueCalcId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpellTooltipInfo that)) return false;
        return mana == that.mana && energy == that.energy && cooldown_ticks == that.cooldown_ticks
                && charge_cooldown_ticks == that.charge_cooldown_ticks && cast_ticks == that.cast_ticks
                && recovery_ticks == that.recovery_ticks && Float.compare(crit_chance, that.crit_chance) == 0
                && Float.compare(crit_damage, that.crit_damage) == 0 && dmg.equals(that.dmg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dmg, mana, energy, cooldown_ticks, charge_cooldown_ticks, cast_ticks, recovery_ticks, crit_chance, crit_damage);
    }
}
