package com.robertx22.mine_and_slash.database.data.wizard;

import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The wind up of one wizard spell cast, driven by {@link WizardSpellCaster}.
 * <p>
 * The same job {@code MercenaryCastState} does, minus its approach half. A mercenary walks skills
 * into range because it is a companion following an owner and can afford the two seconds; a wizard
 * is held at its skill's own distance by {@code WizardCombatGoal} instead, and has no basic attack
 * to fall back on while it walks - so a walk-in phase would just be dead time.
 * <p>
 * Not saved. A cast that survived a reload would resume against a target that no longer exists, and
 * a monster's fight does not outlive a chunk unload in any meaningful way.
 */
public class WizardCastState {

    @Nullable
    public Spell spell;
    @Nullable
    public LivingEntity target;
    /** ticks of wind up still to go before the skill fires */
    public int ticksLeft;
    /** ticks of wind up already spent - what {@code times_to_cast} paces its repeats against */
    public int ticksDone;
    public int totalTicks;

    /**
     * How close the wizard wants to be while this skill is going off, in blocks. The combat goal
     * reads it and tightens its kite band to match, so a five block skill isn't fled out of halfway
     * through its own cast.
     */
    public double engageRange;

    public boolean isCasting() {
        return spell != null;
    }

    public void start(Spell spell, LivingEntity target, int castTimeTicks, double engageRange) {
        this.spell = spell;
        this.target = target;
        this.engageRange = engageRange;
        // a cast time of 0 would finish on the tick it started and divide by zero in the
        // times_to_cast pacing. instant skills never get here, but a datapack can author anything
        this.totalTicks = Math.max(1, castTimeTicks);
        this.ticksLeft = this.totalTicks;
        this.ticksDone = 0;
    }

    public void clear() {
        this.spell = null;
        this.target = null;
        this.engageRange = 0;
        this.ticksLeft = 0;
        this.ticksDone = 0;
        this.totalTicks = 0;
    }

    /** the range of the skill in progress, or 0 when the wizard has none */
    public double activeEngageRange() {
        return spell != null ? engageRange : 0;
    }
}
