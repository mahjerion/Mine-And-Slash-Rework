package com.robertx22.mine_and_slash.capability.entity;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SummonType;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SummonPetAction;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class SummonedPetData {

    public String spell = "";
    public int ticks = 0;
    public int aggro_radius = 10;
    public String summon_type = SummonType.NONE.id;
    public boolean counts_towards_max_summons = true;
    public int ticks_left_to_check_owner = 0;

    /**
     * Who cast the summon, when that isn't the owner - which today means a mercenary.
     * <p>
     * A pet has two grown ups. The OWNER is still always a Player, because that is the only thing
     * vanilla lets an owner be: {@code TamableAnimal.getOwner()} resolves the uuid through
     * {@code level.getPlayerByUUID}, so a mercenary owner would simply never resolve. Everything
     * hanging off ownership therefore keeps working untouched - kill credit, loot, the owner side
     * registry {@link #registeredWithOwner} checks against, and every ally test.
     * <p>
     * The SUMMONER is whose stats the pet runs on and who its attack spell is cast by. Left empty
     * for the ordinary case of a player summoning their own pet, so those serialize exactly as
     * they always did.
     */
    public String summoner_uuid = "";

    public void setup(Spell spell, int ticks, int aggro_radius, boolean counts_towards_max_summons) {
        this.spell = spell.GUID();
        this.ticks = ticks;
        this.aggro_radius = aggro_radius;
        // the spell config is what the stat calc event feeds the summon type conditions,
        // so the cap and the summons counted against it can never disagree
        this.summon_type = spell.config.summonType.id;
        this.counts_towards_max_summons = counts_towards_max_summons;
    }

    public void setup(Spell spell, int ticks, int aggro_radius, boolean counts_towards_max_summons, LivingEntity summoner, LivingEntity owner) {
        setup(spell, ticks, aggro_radius, counts_towards_max_summons);

        if (summoner != null && summoner != owner) {
            this.summoner_uuid = summoner.getStringUUID();
        }
    }

    /**
     * Whose stats this pet runs on: the mercenary that cast it, or the owner when nothing else did.
     * <p>
     * Falls back to the owner whenever the summoner can't be produced - never recorded, dismissed,
     * dead, or in another dimension - so callers need no null handling beyond the one they already
     * had for the owner.
     */
    public LivingEntity getSummoner(LivingEntity summon) {
        LivingEntity owner = summon instanceof SummonEntity se ? se.getOwner() : null;

        if (summoner_uuid == null || summoner_uuid.isEmpty()) {
            return owner;
        }
        if (!(summon.level() instanceof ServerLevel sl)) {
            return owner;
        }
        try {
            Entity en = sl.getEntity(UUID.fromString(summoner_uuid));
            if (en instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        } catch (Exception e) {
            // a malformed uuid out of a hand edited save. the owner is a perfectly good answer.
        }
        return owner;
    }

    public boolean isSummonedBy(LivingEntity entity) {
        return entity != null && summoner_uuid != null && !summoner_uuid.isEmpty()
                && summoner_uuid.equals(entity.getStringUUID());
    }

    public boolean isEmpty() {
        return spell.isEmpty();
    }

    public Spell getSourceSpell() {
        return ExileDB.Spells().get(spell);
    }

    public SummonType getSummonType() {
        if (summon_type != null && !summon_type.isEmpty()) {
            return SummonType.fromId(summon_type);
        }
        // summons spawned before summon_type was saved
        Spell spell = getSourceSpell();
        return spell == null ? SummonType.NONE : spell.config.summonType;
    }


    public void tick(LivingEntity en) {
        if (!en.level().isClientSide) {
            var registeredWithOwner = registeredWithOwner(en);

            if (
                ticks != SummonPetAction.INFINITE_DURATION && ticks-- < 1
                || !registeredWithOwner
            ) {
                SoundUtils.playSound(en, SoundEvents.GENERIC_DEATH);
                discard(en);
            }
        }
    }

    private boolean registeredWithOwner(LivingEntity en) {
        if (ticks_left_to_check_owner-- > 0) {
            return true;
        }
        ticks_left_to_check_owner = 100;

        if (!(en instanceof SummonEntity summonEntity) || summonEntity.getOwner() == null || !(summonEntity.getOwner() instanceof Player player)) {
            return false;
        }

        return Load.player(player).getSummonedData().isOwnBySpell(spell, en.getUUID());
    }

    public void discard(LivingEntity en) {
        en.discard();

        if (!(en instanceof SummonEntity summonEntity)) {
            return;
        }

        onDeath(summonEntity);
    }

    public void onDeath(SummonEntity summonEntity) {
        if (summonEntity.getOwner() == null || !(summonEntity.getOwner() instanceof Player player)) {
            return;
        }

        Load.player(player).removeSummon(spell, summonEntity.getUUID());
    }
}
