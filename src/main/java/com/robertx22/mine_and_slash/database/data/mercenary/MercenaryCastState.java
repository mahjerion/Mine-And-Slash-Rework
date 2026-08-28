package com.robertx22.mine_and_slash.database.data.mercenary;

import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The wind up of one mercenary spell cast, driven by {@link MercenarySpellCaster}.
 * <p>
 * This is the mercenary's stand-in for the three cast fields on {@code SpellCastingData}
 * ({@code castTickLeft} / {@code castTicksDone} / {@code calcSpell}). That class can't be reused -
 * it lives on PlayerData, reads key inputs and syncs to a client - but a mercenary needs the same
 * thing it provides: a skill authored with a cast time has to spend that time winding up before it
 * goes off, instead of firing on the tick it was picked.
 * <p>
 * Deliberately not saved anywhere. {@link com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity#shouldBeSaved()}
 * is false and the entity is respawned from the owner's {@code MercenaryData} on login, so a cast
 * that survived a reload would resume against a target that no longer exists.
 */
public class MercenaryCastState {

    @Nullable
    public Spell spell;
    @Nullable
    public LivingEntity target;
    /** ticks of wind up still to go before the skill fires */
    public int ticksLeft;
    /** ticks of wind up already spent - what {@code times_to_cast} paces its repeats against */
    public int ticksDone;
    public int totalTicks;

    public boolean isCasting() {
        return spell != null;
    }

    public void start(Spell spell, LivingEntity target, int castTimeTicks) {
        this.spell = spell;
        this.target = target;
        // a cast time of 0 would finish on the tick it started and divide by zero in the
        // times_to_cast pacing. instant skills never get here, but a datapack can author anything
        this.totalTicks = Math.max(1, castTimeTicks);
        this.ticksLeft = this.totalTicks;
        this.ticksDone = 0;
    }

    public void clear() {
        this.spell = null;
        this.target = null;
        this.ticksLeft = 0;
        this.ticksDone = 0;
        this.totalTicks = 0;
    }

    // ------------------------------------------------------------------ closing in to cast

    /**
     * The walk in before a cast, for a skill whose own radius does not reach the target yet.
     * <p>
     * Separate fields from the wind up above rather than a phase flag on it, because the two answer
     * different questions: {@code MercenaryEntity.isCastingSpell()} suppresses the mercenary's
     * attacks, which an approach must not do - it is still fighting while it walks - and
     * {@code isApproaching()} instead tells the combat goals to leave the navigation alone.
     */
    @Nullable
    public Spell approachSpell;
    @Nullable
    public LivingEntity approachTarget;
    /** ticks left before the mercenary gives up on getting there and lets another skill have a go */
    public int approachTicksLeft;
    /** squared distance the skill actually reaches, so the arrival check is a bare compare */
    public double approachRangeSqr;

    public boolean isApproaching() {
        return approachSpell != null;
    }

    public void startApproach(Spell spell, LivingEntity target, double rangeSqr, int timeoutTicks) {
        this.approachSpell = spell;
        this.approachTarget = target;
        this.approachRangeSqr = rangeSqr;
        this.approachTicksLeft = timeoutTicks;
    }

    public void clearApproach() {
        this.approachSpell = null;
        this.approachTarget = null;
        this.approachRangeSqr = 0;
        this.approachTicksLeft = 0;
    }
}
