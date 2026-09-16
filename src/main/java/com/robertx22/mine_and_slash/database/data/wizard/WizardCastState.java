package com.robertx22.mine_and_slash.database.data.wizard;

import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The wind up of one wizard spell cast, driven by {@link WizardSpellCaster}.
 * <p>
 * The same job {@code MercenaryCastState} does, minus its approach half. A mercenary walks skills
 * into range because it is a companion following an owner and can afford the two seconds; a wizard
 * is held at its skill's own distance by {@code WizardCombatGoal} instead, and has no basic attack
 * to fall back on while it walks - so a walk-in phase would just be dead time.
 * <p>
 * Two phases. First a <b>telegraph</b>: the skill's icon fills over the wizard's head and its area is
 * drawn on the ground, so a player can see what is coming and where. Then the skill's own <b>cast
 * time</b>, exactly as before - the phases are kept apart so {@code times_to_cast} repeats still pace
 * against the cast time alone, and a multicast's beat isn't smeared across the telegraph.
 * <p>
 * Not saved. A cast that survived a reload would resume against a target that no longer exists, and
 * a monster's fight does not outlive a chunk unload in any meaningful way.
 */
public class WizardCastState {

    @Nullable
    public Spell spell;
    @Nullable
    public LivingEntity target;

    /** ticks of telegraph still to go before the skill's own cast time starts */
    public int telegraphTicksLeft;
    /** the skill's own cast time, held back until the telegraph is over */
    public int castTimeTicks;

    /** ticks of cast time still to go before the skill fires. 0 during the telegraph */
    public int ticksLeft;
    /** ticks of cast time already spent - what {@code times_to_cast} paces its repeats against */
    public int ticksDone;
    public int totalTicks;

    /**
     * Where an at-sight skill will land, frozen when the telegraph started, or null for any other
     * shape. The circle drawn on the ground is drawn here, so the skill has to land here too - that
     * is what makes the telegraph something a player can walk out of.
     * <p>
     * Captured once per cast. A multicast at-sight skill would put every repeat on this one spot,
     * which is the honest reading of a single telegraph.
     */
    @Nullable
    public Vec3 lockedPos;

    /**
     * How close the wizard wants to be while this skill is going off, in blocks. The combat goal
     * reads it and tightens its kite band to match, so a five block skill isn't fled out of halfway
     * through its own cast - or through its telegraph, which is set up with it.
     */
    public double engageRange;

    public boolean isCasting() {
        return spell != null;
    }

    public boolean isTelegraphing() {
        return spell != null && telegraphTicksLeft > 0;
    }

    public void startTelegraph(Spell spell, @Nullable LivingEntity target, int telegraphTicks, int castTimeTicks,
                               double engageRange, @Nullable Vec3 lockedPos) {
        this.spell = spell;
        this.target = target;
        this.engageRange = engageRange;
        this.lockedPos = lockedPos;
        this.telegraphTicksLeft = Math.max(0, telegraphTicks);
        this.castTimeTicks = castTimeTicks;
        this.ticksLeft = 0;
        this.ticksDone = 0;
        this.totalTicks = 0;
    }

    /** the telegraph is over, start the skill's own cast time */
    public void beginCastPhase() {
        this.telegraphTicksLeft = 0;
        // a cast time of 0 would finish on the tick it started and divide by zero in the
        // times_to_cast pacing. instant skills never get here, but a datapack can author anything
        this.totalTicks = Math.max(1, castTimeTicks);
        this.ticksLeft = this.totalTicks;
        this.ticksDone = 0;
    }

    public void clear() {
        this.spell = null;
        this.target = null;
        this.lockedPos = null;
        this.engageRange = 0;
        this.telegraphTicksLeft = 0;
        this.castTimeTicks = 0;
        this.ticksLeft = 0;
        this.ticksDone = 0;
        this.totalTicks = 0;
    }

    /** the range of the skill in progress, or 0 when the wizard has none */
    public double activeEngageRange() {
        return spell != null ? engageRange : 0;
    }
}
