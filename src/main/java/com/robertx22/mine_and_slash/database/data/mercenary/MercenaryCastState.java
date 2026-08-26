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
}
