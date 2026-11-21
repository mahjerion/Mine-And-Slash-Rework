package com.robertx22.mine_and_slash.uncommon.stat_calculation;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.mine_and_slash.capability.player.helper.GemInventoryHelper;
import com.robertx22.mine_and_slash.database.data.spells.components.ComponentPart;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.database.data.value_calc.ValueCalculation;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.saveclasses.spells.SpellTooltipInfo;
import com.robertx22.mine_and_slash.saveclasses.spells.SpellTooltipInfos;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EventBuilder;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Works out what a spell's tooltip should say by actually running the spell through the same code a
 * cast does, then throwing the result away instead of applying it.
 * <p>
 * This has to happen on the server: the per spell stat unit (the one support gems are folded into)
 * is only ever calculated there, and so is the stat effect sweep that turns a spell's base damage
 * into the damage a hit really deals. The client is handed the finished numbers - see
 * {@link SpellTooltipInfos} and SyncSpellTooltipsPacket.
 * <p>
 * What the numbers do and do not include is deliberate. Included: every increase, more multiplier
 * and reduction the caster has for this hit, flat added damage, and conversion into other elements.
 * Excluded: the target's mitigation (armor, resistances, block, dodge) since there is no target, and
 * crits - every random roll reads as a miss in a simulation, so the damage is the plain hit and the
 * crit chance is reported on its own.
 */
public class SpellTooltipCalculation {

    public static String CRIT_CHANCE_ID = "critical_hit";
    public static String CRIT_DAMAGE_ID = "critical_damage";

    // this runs up to once a second per player, so a spell that fails would print the same stack
    // trace forever. report each one once per session
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private static void report(String key, Exception e) {
        if (REPORTED.add(key)) {
            ExileLog.get().warn("Spell tooltip calculation failed for " + key + ", the tooltip will show base numbers");
            e.printStackTrace();
        }
    }

    public static SpellTooltipInfos calcAll(Player p) {

        SpellTooltipInfos all = new SpellTooltipInfos();

        var inv = Load.player(p).getSkillGemInventory();

        for (int i = 0; i < GemInventoryHelper.MAX_SKILL_GEMS; i++) {
            try {
                var gem = inv.getIndexGem(i);
                if (gem == null) {
                    continue;
                }
                Spell spell = gem.getSpell();
                if (spell == null || all.map.containsKey(spell.GUID())) {
                    continue;
                }
                all.map.put(spell.GUID(), calc(p, spell));
            } catch (Exception e) {
                report("gem slot " + i, e);
            }
        }

        return all;
    }

    public static SpellTooltipInfo calc(Player p, Spell spell) {

        SpellTooltipInfo info = new SpellTooltipInfo();

        SpellCastContext ctx = new SpellCastContext(p, 0, spell, true);

        info.mana = spell.getCalculatedManaCost(ctx);
        info.energy = spell.getCalculatedEnergyCost(ctx);
        info.cooldown_ticks = spell.getCooldownTicks(ctx);
        info.charge_cooldown_ticks = spell.getChargeCooldownTicks(ctx);
        info.cast_ticks = spell.getCastTimeTicks(ctx);
        info.recovery_ticks = spell.getCastSpeedTicks(ctx);

        if (ctx.unit != null) {
            info.crit_chance = ctx.unit.getCalculatedStat(CRIT_CHANCE_ID).getValue();
            info.crit_damage = ctx.unit.getCalculatedStat(CRIT_DAMAGE_ID).getValue();
        }

        for (ComponentPart part : spell.getAttached().getAllComponents()) {
            for (MapHolder act : part.acts) {
                if (!act.type.equals(SpellAction.DEAL_DAMAGE.GUID())) {
                    continue;
                }
                try {
                    ValueCalculation calc = act.get(MapField.VALUE_CALCULATION);
                    if (calc == null || info.dmg.containsKey(calc.GUID())) {
                        // a spell description writes [calc:id], so the first damage action using a
                        // calculation is the one it can be showing
                        continue;
                    }
                    info.dmg.put(calc.GUID(), simulateHit(p, spell, calc, act));
                } catch (Exception e) {
                    report(spell.GUID() + " damage", e);
                }
            }
        }

        return info;
    }

    // mirrors DamageAction.tryActivate, minus everything that needs a real target
    private static int simulateHit(Player p, Spell spell, ValueCalculation calc, MapHolder act) {

        Elements ele = act.getElement();

        int base = calc.getCalculatedValue(p, null, spell);
        float dmgEffectiveness = calc.getDamageEffectiveness(p, spell);

        DamageEvent dmg = EventBuilder.ofSpellDamage(p, p, base, spell).build();

        // the caster stands in as their own target only because an event needs one. the target half
        // of the sweep is off, so none of their own defenses touch the number
        dmg.simulation = true;
        dmg.calcTargetEffects = false;
        dmg.disableActivation = true;

        dmg.data.setupNumber(EventData.DMG_EFFECTIVENESS, dmgEffectiveness);

        if (spell.usesWeaponForDamage()) {
            GearItemData gear = Load.Unit(p).equipmentCache.getWeaponOpt().map(x -> x.gear).orElse(null);
            if (gear != null) {
                dmg.data.setString(EventData.WEAPON_TYPE, gear.GetBaseGearType().weapon_type);
            }
        }
        if (act.has(MapField.DMG_EFFECT_TYPE)) {
            dmg.data.setString(EventData.ATTACK_TYPE, act.getDmgEffectType().name());
        }

        dmg.setElement(ele);
        dmg.Activate();

        // the total, not just the main element - conversion and "x as extra y" move part of a hit
        // into other elements and the player is still dealing all of it
        return (int) dmg.calculateAllBonusElementalDamage().totalDmg;
    }
}
