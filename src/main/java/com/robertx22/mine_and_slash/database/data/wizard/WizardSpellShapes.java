package com.robertx22.mine_and_slash.database.data.wizard;

import com.robertx22.mine_and_slash.database.data.spells.components.ComponentPart;
import com.robertx22.mine_and_slash.database.data.spells.components.MapHolder;
import com.robertx22.mine_and_slash.database.data.spells.components.ProjectileCastHelper;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.TargetSelector;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * What a wizard skill reaches and what it looks like on the ground, worked out once from its own
 * components.
 * <p>
 * One scan answers two questions. {@code WizardSpellCaster.engageRange} asks how close the wizard
 * has to stand, and the telegraph asks what to draw: a disc under the target, a disc around the
 * wizard, or a fan of lines. Both are read off the same components, so they are read off the same
 * walk - two scans would be two caches that could disagree about the same skill.
 * <p>
 * <b>Server thread only.</b> The client never calls this: the server scales the numbers by the
 * wizard's stats and syncs the result, see {@code WizardEntity.publishCast}. Adding a client caller
 * would make the unsynchronised cache below a race.
 */
public class WizardSpellShapes {

    public enum TelegraphKind {
        /** nothing aimed at an enemy that has a shape worth drawing - the icon still shows */
        NONE,
        /** an area centred on the wizard, e.g. a nova */
        SELF_CIRCLE,
        /** dropped onto the target's position, e.g. a meteor. the landing spot is locked */
        AT_TARGET_CIRCLE,
        /** thrown along the wizard's aim, one line per projectile */
        PROJECTILE_LINE
    }

    /**
     * {@code unlimited} covers the two cases with no distance to close: an action that lands on the
     * target's own position, and a skill with nothing aimed at an enemy anywhere in it.
     * <p>
     * {@code rawSize} is the telegraph's radius or line length straight off the components, before the
     * caster's area or projectile speed stats. {@code projCount}/{@code projApart} describe the fan the
     * way {@link ProjectileCastHelper} spreads it, also before bonus projectiles.
     */
    public record Analysis(boolean unlimited, double onCastRange, double projTravel, double detonationRadius,
                           TelegraphKind kind, double rawSize, int projCount, double projApart,
                           boolean ignoresBonusProjectiles) {
    }

    /** smallest disc drawn for an at-sight skill that declares no area of its own */
    private static final double MIN_AT_TARGET_RADIUS = 1.5D;

    /** the default {@code SummonProjectileAction} applies when a projectile doesn't author one */
    private static final double DEFAULT_PROJ_APART = 75D;

    /**
     * Computed once per spell - a function of datapack data that does not change while loaded.
     * <p>
     * Keyed on the Spell instance rather than its id, and weak, so a datapack reload invalidates it
     * for free: the reload builds new Spell objects, the old ones become unreachable, and their
     * entries go with them. An id-keyed cache would serve a pre-reload radius forever.
     */
    private static final Map<Spell, Analysis> CACHE = new WeakHashMap<>();

    public static Analysis of(Spell spell) {
        Analysis cached = CACHE.get(spell);
        if (cached == null) {
            cached = analyse(spell);
            CACHE.put(spell, cached);
        }
        return cached;
    }

    private static Analysis analyse(Spell spell) {

        double onCastRange = 0;
        double inFrontDistance = 0;
        double projTravel = 0;
        int projCount = 0;
        double projApart = DEFAULT_PROJ_APART;
        boolean ignoresBonus = false;
        boolean huntsAnything = false;
        // not an early return: the radius an at-sight skill hits with lives on the entity it leaves
        // behind, which is the scan further down
        boolean atSight = false;

        for (ComponentPart part : spell.attached.on_cast) {

            for (MapHolder act : part.acts) {
                if (SpellAction.SUMMON_AT_SIGHT.GUID().equals(act.type)) {
                    // for anything that isn't a player this drops straight onto the target's own
                    // position - see SummonAtSightAction - so there is no distance to close
                    atSight = true;
                }
                if (SpellAction.SUMMON_PROJECTILE.GUID().equals(act.type)) {
                    huntsAnything = true;
                    double life = act.getOrDefault(MapField.LIFESPAN_TICKS, 0D);
                    double speed = act.getOrDefault(MapField.PROJECTILE_SPEED, 0D);
                    // real flight, after vanilla per-tick air drag - the naive life * speed
                    // overshoots by ~25% on slow long-lived shots like frozen orb
                    double travel = ProjectileCastHelper.travelDistance(speed, life);
                    if (travel >= projTravel) {
                        projTravel = travel;
                        projCount = Math.max(1, act.getOrDefault(MapField.PROJECTILE_COUNT, 1D).intValue());
                        projApart = act.getOrDefault(MapField.PROJECTILES_APART, DEFAULT_PROJ_APART);
                        ignoresBonus = act.getOrDefault(MapField.IGNORE_BONUS_PROJECTILES, false);
                    }
                }
            }

            for (MapHolder sel : part.targets) {
                // only what the skill is trying to hit sets how close it has to be, and only a
                // selector that describes a DISTANCE says anything about it - a self or target
                // selector hits what it hits from wherever the caster is standing.
                if (!huntsEnemies(sel)) {
                    continue;
                }
                if (TargetSelector.AOE.GUID().equals(sel.type)) {
                    huntsAnything = true;
                    onCastRange = Math.max(onCastRange, sel.getOrDefault(MapField.RADIUS, 0D));
                } else if (TargetSelector.IN_FRONT.GUID().equals(sel.type)) {
                    huntsAnything = true;
                    double dist = sel.getOrDefault(MapField.DISTANCE, 0D);
                    onCastRange = Math.max(onCastRange, dist);
                    inFrontDistance = Math.max(inFrontDistance, dist);
                }
            }
        }

        // the widest enemy hunting area on whatever the skill leaves behind. an orb that travels six
        // blocks and detonates for four can hit something ten blocks away, and a field that never
        // moves reaches exactly its own radius.
        double detonation = 0;
        boolean entitiesHuntEnemies = false;

        for (List<ComponentPart> parts : spell.attached.entity_components.values()) {
            for (ComponentPart part : parts) {
                for (MapHolder sel : part.targets) {
                    if (!huntsEnemies(sel)) {
                        continue;
                    }
                    entitiesHuntEnemies = true;
                    if (TargetSelector.AOE.GUID().equals(sel.type)) {
                        detonation = Math.max(detonation, sel.getOrDefault(MapField.RADIUS, 0D));
                    }
                }
            }
        }

        if (atSight) {
            // aimed at an enemy, but with no distance to close - it lands on the target itself
            return new Analysis(true, 0, 0, 0, TelegraphKind.AT_TARGET_CIRCLE,
                    Math.max(detonation, MIN_AT_TARGET_RADIUS), 0, DEFAULT_PROJ_APART, false);
        }

        if (!huntsAnything && !entitiesHuntEnemies) {
            // nothing in the skill is aimed at an enemy
            return new Analysis(true, 0, 0, 0, TelegraphKind.NONE, 0, 0, DEFAULT_PROJ_APART, false);
        }

        TelegraphKind kind = TelegraphKind.NONE;
        double size = 0;

        if (projTravel > 0) {
            kind = TelegraphKind.PROJECTILE_LINE;
            size = projTravel;
        } else if (inFrontDistance > 0) {
            // a cone in front of the caster reads closest to a single line of its length
            kind = TelegraphKind.PROJECTILE_LINE;
            size = inFrontDistance;
            projCount = 1;
        } else if (onCastRange > 0) {
            kind = TelegraphKind.SELF_CIRCLE;
            size = onCastRange;
        }

        return new Analysis(false, onCastRange, projTravel, detonation, kind, size, projCount, projApart, ignoresBonus);
    }

    /**
     * Whether this selector is looking for something to hit rather than something to help.
     * <p>
     * Written as a list of the ally predicates rather than of the hostile ones on purpose: a
     * predicate nobody thought of here counts as hostile, which costs the wizard a few steps it did
     * not need to take. Guessing the other way would put it back to casting at nothing.
     */
    private static boolean huntsEnemies(MapHolder selector) {
        try {
            AllyOrEnemy pred = selector.getEntityPredicate();
            return pred != AllyOrEnemy.allies
                    && pred != AllyOrEnemy.allies_not_self
                    && pred != AllyOrEnemy.pets
                    && pred != AllyOrEnemy.casters_summons;
        } catch (Exception e) {
            // no predicate declared at all - assume it is aimed at something
            return true;
        }
    }
}
