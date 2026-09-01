package com.robertx22.mine_and_slash.event_hooks.damage_hooks;

import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class PetAttackUTIL {

    public static void tryAttack(LivingEntity summon, LivingEntity caster, LivingEntity target) {

        if (caster != null) {

            Spell spell = ExileDB.Spells().get(Load.Unit(summon).summonedPetData.spell);

            if (spell != null) {


                Spell basic = spell.getConfig().getSummonBasicSpell();

                var ctx = new SpellCastContext(caster, 0, basic);

                //  var originctx = new SpellCastContext(caster, 0, basic); // pet should be using the pet spell here


                boolean cancast = false;
                // a mercenary pays nothing and has no casting state to consult - the same reason
                // MercenarySpellCaster never calls spendResources either. without this branch
                // cancast stayed false for every non player caster, so a mercenary's pet bit for
                // exactly zero damage forever (the vanilla hit is cancelled by the caller).
                boolean spendsResources = caster instanceof Player;

                if (caster instanceof Player p) {
                    cancast = Load.player(p).spellCastingData.canCast(basic, p).can;
                } else if (caster instanceof MercenaryEntity) {
                    cancast = true;
                }

                if (cancast) {
                    if (spendsResources) {
                        basic.spendResources(ctx);
                    }
                    basic.attached.onCast(SpellCtx.onCast(caster, ctx.calcData));
                    basic.attached.tryActivate(Spell.DEFAULT_EN_NAME, SpellCtx.onHit(caster, summon, target, ctx.calcData)); // todo this should be reworked.
                    // pet ability should gain the stats of the pet used to summon it, this is a nasty hack
                }

            }
        } else {
            summon.kill();
        }
    }
}
