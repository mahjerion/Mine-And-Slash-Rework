package com.robertx22.mine_and_slash.database.data.spells.summons;

import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.SpellConfiguration;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.database.data.stats.types.summon.GolemSpellChance;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * The spells a summon casts beyond its basic attack, read off the skill that summoned it.
 * <p>
 * This replaces {@code GolemSummon.aoeSpell()}, which was an abstract Java method returning one
 * hard-coded spell id. Nothing about that was reachable from a datapack: a pack could not change what
 * a golem cast, could not give it a second spell, and could not give a zombie, skeleton, spider or
 * wolf any spell at all, because those entity classes simply had no such method to override. The list
 * now lives on {@link SpellConfiguration#summon_spells}, beside the {@code summon_basic_atk} that was
 * already authored there, so every summon skill in the game gained the capability at once.
 * <p>
 * Trigger and pacing are deliberately unchanged from the golem behaviour they generalise - a roll when
 * the summon lands a hit, gated by a cooldown on the summon itself - so moving the nova out of Java
 * does not also change how a golem plays. What the roll is against is now
 * {@link SpellConfiguration#summon_spell_chance} plus, for golems only, the owner's Golem Spell Chance
 * stat, which keeps every unique, perk and implicit that grants that stat doing exactly what it did.
 */
public class SummonSpellCaster {

    /** the summon's own cooldown key. one key for the whole list, so two spells cannot chain-fire */
    private static final String CD_KEY = "summon_spell";

    /**
     * Rolls for, and casts, one of this summon's extra spells - called once per hit it lands.
     * <p>
     * The summoner is who the spell is cast BY: its stats score the spell, and for a mercenary's pet
     * that is the mercenary rather than the owning player, matching the basic attack in
     * {@code PetAttackUTIL}. The summon is only the source entity, which is what puts the spell at the
     * summon's feet and marks the damage as the summon's - see the note on {@link #fire}.
     *
     * @param target what the summon just hit, so a targeted spell in the list has somewhere to go
     */
    public static void tryCastOnHit(LivingEntity summon, LivingEntity summoner, LivingEntity target) {

        try {
            if (summon == null || summoner == null || summon.level().isClientSide) {
                return;
            }

            Spell source = Load.Unit(summon).summonedPetData.getSourceSpell();

            if (source == null || !source.getConfig().hasSummonSpells()) {
                return;
            }

            SpellConfiguration config = source.getConfig();

            var cds = Load.Unit(summon).getCooldowns();

            if (cds.isOnCooldown(CD_KEY)) {
                return;
            }

            int chance = config.summon_spell_chance + golemBonus(source, summoner);

            if (chance < 1) {
                return; // nothing to roll, and no reason to spend a cooldown on a certain failure
            }

            // the cooldown is stamped on the attempt rather than on the success, exactly as the golem
            // path did. otherwise a low chance summon rolls every single hit, and a fast attacker with
            // a bad roll streak gets a burst of casts the moment the streak breaks.
            cds.setOnCooldown(CD_KEY, Math.max(1, config.summon_spell_cd_ticks));

            if (!RandomUtils.roll(chance)) {
                return;
            }

            List<Spell> spells = config.getSummonSpells();

            if (spells.isEmpty()) {
                return; // every id in the list is unknown to the registry
            }

            fire(summon, summoner, target, RandomUtils.randomFromList(spells));

        } catch (Exception e) {
            // this runs from inside a damage event. a broken datapack entry must never take the hit
            // that triggered it down with it.
            e.printStackTrace();
        }
    }

    /**
     * Casts the spell as the summoner, from the summon's position.
     * <p>
     * The split matters. The caster is the summoner because that is whose stats the spell is scored
     * against - the same hack {@code PetAttackUTIL} uses for the basic attack. The source entity is the
     * summon, and that is what {@code DamageAction} checks to set {@code petEntity} and
     * {@code IS_SUMMON_ATTACK}, so Summon Damage stats apply and threat lands on the pet rather than on
     * the player standing behind it. It is also what makes the default {@code PositionSource.SOURCE_ENTITY}
     * resolve to the summon, so a nova goes off around the pet instead of around its owner.
     * <p>
     * The target is set, which the old golem path did not do - it left target pointing at the caster,
     * which is harmless for a self centred nova and wrong for anything aimed. Same line
     * {@code WizardSpellCaster.fire} uses.
     */
    private static void fire(LivingEntity summon, LivingEntity summoner, LivingEntity target, Spell spell) {
        SpellCastContext c = new SpellCastContext(summoner, 0, spell);

        SpellCtx ctx = SpellCtx.onCast(summoner, c.calcData).setSourceEntity(summon);

        if (target != null && target.isAlive()) {
            ctx.target = target;
        }

        spell.getAttached().onCast(ctx);
    }

    /**
     * The owner's Golem Spell Chance, for a golem.
     * <p>
     * Keyed on the summon skill carrying {@link SpellTags#golem} rather than on
     * {@code instanceof GolemSummon}, which is where this check lived before. That keeps the stat
     * exactly as golem-only as it has always been - all three golem skills carry the tag - while
     * letting a pack author a fourth golem without also needing a Java entity class, which is the
     * whole point of the change. Every item, perk and implicit granting the stat is untouched.
     */
    private static int golemBonus(Spell summonSkill, LivingEntity summoner) {
        if (!summonSkill.getConfig().tags.contains(SpellTags.golem)) {
            return 0;
        }
        return (int) Load.Unit(summoner).getUnit().getCalculatedStat(GolemSpellChance.getInstance()).getValue();
    }
}
