package com.robertx22.mine_and_slash.database.data.mercenary;

import com.robertx22.mine_and_slash.capability.entity.CooldownsData;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.PositionSource;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.CastingWeapon;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

/**
 * Drives a mercenary's active skills.
 * <p>
 * Modelled on {@link com.robertx22.mine_and_slash.uncommon.effectdatas.rework.action.ProcSpellEffect},
 * which is the one path in the codebase that casts a spell from an arbitrary LivingEntity with no
 * player assumptions anywhere in it. The player's own state machine ({@code SpellCastingData}) is not
 * reusable here - it lives on PlayerData, reads key inputs, and gates almost everything on
 * {@code instanceof Player}.
 * <p>
 * Skills cost nothing: the design gives mercenaries no mana or energy, so {@code spendResources} is
 * never called and no resource check gates a cast.
 * <p>
 * Cast time is honoured: a skill authored with {@code cast_time_ticks} spends that long winding up in
 * {@link MercenaryCastState} before it goes off, instead of resolving on the tick it was picked. The
 * mercenary keeps walking and kiting through the wind up - only its attacks stop, see
 * {@link MercenaryEntity#isCastingSpell()}.
 */
public class MercenarySpellCaster {

    public static void onTick(MercenaryEntity merc) {

        MercenaryCastState cast = merc.getCastState();

        // a cast in progress owns the mercenary, the same way isCasting() owns the input on the player
        // path. no other skill is picked, and nothing fires, until this one resolves.
        if (cast.isCasting()) {
            tickCast(merc, cast);
            return;
        }

        LivingEntity target = merc.getTarget();

        // "Skills can only be cast while the Mercenary is actively in combat" - no target, no casting.
        if (target == null || !target.isAlive() || !merc.isAlive()) {
            return;
        }
        if (merc.getMode() == MercenaryData.CombatMode.IDLE) {
            return;
        }

        MercenaryData data = merc.getMercData();
        if (data == null) {
            return;
        }

        EntityData unit = Load.Unit(merc);

        // one skill at a time, same as a player - without this every off cooldown skill would fire on
        // the same tick the moment a target appeared.
        if (unit.getCooldowns().isOnCooldown(CooldownsData.GLOBAL_COOLDOWN)) {
            return;
        }

        for (int i = 0; i < MercenaryClass.EQUIPPED_SKILLS; i++) {
            Spell spell = data.getEquippedSpell(i);
            if (spell == null) {
                continue;
            }
            // a skill the mercenary has slotted but has since dropped below the level for, or one it
            // is not holding the right weapon for
            if (!isUsable(merc, data, spell)) {
                continue;
            }
            if (unit.getCooldowns().isOnCooldown(spell.GUID())) {
                continue;
            }

            beginCast(merc, spell, target);
            // the list order IS the priority queue, so the first castable skill wins and the rest
            // wait for the next opening.
            return;
        }
    }

    private static boolean isUsable(MercenaryEntity merc, MercenaryData data, Spell spell) {
        MercenaryClass mc = data.getMercClass();
        if (mc == null || !mc.isSkillUnlocked(spell.GUID(), data.lvl)) {
            return false;
        }
        return hasCastingWeapon(merc, spell);
    }

    /**
     * The weapon gate a player passes through in {@code SpellCastingData.canCast}. That check lives on
     * PlayerData, so it never ran here - a mercenary's skills advertised "Requires Mage Weapon" in the
     * mercenary screen (the tooltip comes from {@code Spell.GetTooltipString}) while it cast them bare
     * handed. Nothing about the predicates is player specific: they are typed to LivingEntity and read
     * {@code equipmentCache.getWeapon()}, which a mercenary has once MercenaryManager.applyGear has
     * mirrored its inventory onto the entity.
     * <p>
     * Only the declared {@code castingWeapon} predicate, deliberately not the player path's extra
     * "must be holding an MnS weapon at all" rule - {@link CastingWeapon#ANY_WEAPON} has to stay
     * castable by a freshly hired mercenary, or one would stand there doing nothing until geared.
     */
    private static boolean hasCastingWeapon(MercenaryEntity merc, Spell spell) {
        if (CompatConfig.get().ignoreWeaponReqForSpells()) {
            return true;
        }
        CastingWeapon req = spell.getConfig().castingWeapon;

        if (req.predicate.predicate.test(merc)) {
            return true;
        }
        // a battlemage casts magic off a melee weapon - the same exception the player path makes
        return req == CastingWeapon.MAGE_WEAPON && Load.Unit(merc).getUnit().isBattlemage();
    }

    /**
     * How long the skill itself is locked out for after a cast.
     * <p>
     * A charge skill has no cooldown of its own on purpose - {@code setChargesAndRegen} zeroes it
     * because the charge pool is meant to be the limiter - but that pool is {@code ChargeData} on
     * PlayerData, which a mercenary has none of. With nothing standing in for it the skill came back
     * on its bare recovery: merc_meteor is authored as 3 charges on a 20s regen and was firing every
     * 1.5 seconds. Pacing it by the regen time instead gives the rate the datapack asked for. It is a
     * steady one-per-regen rather than a real three-cast burst - that would need ChargeData decoupled
     * from Player and a sync path of its own.
     */
    private static int cooldownTicksFor(Spell spell, SpellCastContext ctx) {
        int cd = spell.getEffectiveCooldownTicks(ctx);
        if (spell.config.charges > 0) {
            return Math.max(cd, spell.getChargeCooldownTicks(ctx));
        }
        return cd;
    }

    /**
     * How long this skill winds up for before it goes off.
     * <p>
     * A channel is deliberately treated as instant here: its {@code cast_time_ticks} is the gap
     * between pulses rather than a wind up, and keeping one going needs an input held down, which a
     * mercenary has no equivalent of. It fires once and pays its recovery like every other skill.
     */
    private static int castTimeTicksFor(Spell spell, SpellCastContext ctx) {
        if (spell.getConfig().isChannel()) {
            return 0;
        }
        return spell.getCastTimeTicks(ctx);
    }

    /**
     * Arms the skill and either fires it immediately (an instant) or starts its wind up.
     * <p>
     * Both cooldowns go on here, at the START of the cast, never at the end. The player path arms them
     * in {@code onSpellCastFinished} instead, but here they are also what stops a wind up the
     * mercenary had to abandon - its target died, its owner switched it to Idle - from being re-picked
     * on the very next tick and locking it into a cast it never finishes.
     */
    private static void beginCast(MercenaryEntity merc, Spell spell, LivingEntity target) {
        try {
            SpellCastContext ctx = new SpellCastContext(merc, 0, spell);
            EntityData unit = Load.Unit(merc);

            unit.getCooldowns().setOnCooldown(spell.GUID(), cooldownTicksFor(spell, ctx));
            unit.getCooldowns().setOnCooldown(CooldownsData.GLOBAL_COOLDOWN, spell.getCastSpeedTicks(ctx));

            // the player's own cast swing is commented out in Spell.cast - players get a proper
            // animation from the Player Animator instead, which a mob has no equivalent of. so the
            // mercenary swings here, respecting the spell's own swing_arm flag. it swings as the cast
            // STARTS, so a skill with a wind up has a tell instead of landing out of nowhere.
            if (spell.config.swing_arm) {
                merc.swing(InteractionHand.MAIN_HAND);
            }

            aimAt(merc, target);

            int castTime = castTimeTicksFor(spell, ctx);

            if (castTime <= 1) {
                fire(merc, spell, ctx, target);
                return;
            }

            merc.getCastState().start(spell, target, castTime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** one tick of a wind up: keep aiming, pace any repeats, fire when the timer runs out */
    private static void tickCast(MercenaryEntity merc, MercenaryCastState cast) {
        try {
            Spell spell = cast.spell;
            LivingEntity target = cast.target;

            // a wind up is not a commitment: it dies with the thing it was aimed at, and an owner
            // flipping to Idle has to stop the mercenary mid cast, not one cast later
            if (spell == null || target == null || !target.isAlive() || target.isRemoved()
                    || !merc.isAlive() || merc.getMode() == MercenaryData.CombatMode.IDLE) {
                cast.clear();
                return;
            }

            cast.ticksLeft--;
            cast.ticksDone++;

            // keep tracking through the wind up, so a target that moved during it is still aimed at
            aimAt(merc, target);

            int timesToCast = spell.getConfig().times_to_cast;

            if (timesToCast > 1) {
                // the pacing {@code Spell.onCastingTick} does, repeated here rather than called: the
                // ctx it hands to Spell.cast builds a bare SpellCtx with no target and no position
                // source, so every repeat would land wherever the mercenary happens to face.
                int castsByLastTick = (cast.ticksDone - 1) * timesToCast / cast.totalTicks;
                int castsByThisTick = cast.ticksDone * timesToCast / cast.totalTicks;

                if (castsByThisTick != castsByLastTick) {
                    fire(merc, spell, new SpellCastContext(merc, cast.ticksDone, spell), target);
                }
            }

            if (cast.ticksLeft <= 0) {
                if (timesToCast <= 1) {
                    fire(merc, spell, new SpellCastContext(merc, cast.ticksDone, spell), target);
                }
                cast.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            cast.clear();
        }
    }

    /** the cast itself - the point the skill's own components actually run */
    private static void fire(MercenaryEntity merc, Spell spell, SpellCastContext ctx, LivingEntity target) {
        // aiming: point the mercenary at what it is fighting and hand the cast an explicit target,
        // so projectiles and targeted spells lock on instead of firing wherever it happens to face.
        aimAt(merc, target);

        SpellCtx c = SpellCtx.onCast(merc, ctx.calcData);
        c.setPositionSource(PositionSource.TARGET);
        c.target = target;

        // last, for the same reason ProcSpellEffect does it last
        spell.attached.onCast(c);
    }

    private static void aimAt(MercenaryEntity merc, LivingEntity target) {
        merc.getLookControl().setLookAt(target, 30F, 30F);
        merc.setYRot(yawTowards(merc, target));
        merc.yHeadRot = merc.getYRot();
        merc.yBodyRot = merc.getYRot();
    }

    private static float yawTowards(LivingEntity from, LivingEntity to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90F);
    }
}
