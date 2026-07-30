package com.robertx22.mine_and_slash.capability.entity;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SummonType;
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
    public String summon_type = SummonType.NONE.id;
    public boolean counts_towards_max_summons = true;
    public int ticks_left_to_check_owner = 0;

    public void setup(Spell spell, int ticks, int aggro_radius, boolean counts_towards_max_summons) {
        this.spell = spell.GUID();
        this.ticks = ticks;
        this.aggro_radius = aggro_radius;
        // the spell config is what the stat calc event feeds the summon type conditions,
        // so the cap and the summons counted against it can never disagree
        this.summon_type = spell.config.summonType.id;
        this.counts_towards_max_summons = counts_towards_max_summons;
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
