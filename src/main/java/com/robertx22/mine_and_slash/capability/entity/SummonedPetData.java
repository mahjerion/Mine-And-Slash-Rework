package com.robertx22.mine_and_slash.capability.entity;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SummonPetAction;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SummonedPetData {

    public String spell = "";
    public int ticks = 0;
    public int aggro_radius = 10;
    public boolean counts_towards_max_summons;
    public int ticks_left_to_check_owner = 0;

    public void setup(Spell spell, int ticks, int aggro_radius, boolean counts_towards_max_summons) {
        this.spell = spell.GUID();
        this.ticks = ticks;
        this.aggro_radius = aggro_radius;
        this.counts_towards_max_summons = counts_towards_max_summons;
    }

    public boolean isEmpty() {
        return spell.isEmpty();
    }

    public Spell getSourceSpell() {
        return ExileDB.Spells().get(spell);
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
