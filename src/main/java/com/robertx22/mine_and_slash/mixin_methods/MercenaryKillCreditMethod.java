package com.robertx22.mine_and_slash.mixin_methods;

import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity;
import com.robertx22.mine_and_slash.mixin_ducks.DamageSourceDuck;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * A mercenary's kill is its owner's kill, for every mod that asks "was this killed by a player".
 * <p>
 * Nearly all of them - FTB Quests, EntityLootDrops, vanilla's own loot conditions - answer that by
 * reading {@code DamageSource.getEntity()}, and a MercenaryEntity is a TamableAnimal, not a Player.
 * So the kill silently didn't count: no quest credit, and in a map the pack's "only Mine and Slash
 * items drop here" filter never engaged, leaving the mob's full vanilla loot table on the floor
 * alongside the mns loot.
 * <p>
 * The swap deliberately happens at {@code LivingEntity.die} and nowhere earlier. Aggro
 * ({@code setLastHurtByMob}) and {@code lastDamageSource} are both written back in
 * {@code hurt()}, so by the time this runs mobs are already locked onto the mercenary and
 * {@code OnMobDeathDrops} can still find it for its Find stats. The mercenary also stays the
 * source's DIRECT entity - only the causing entity moves to the owner, which is exactly vanilla's
 * own indirect-attack shape (arrow direct, shooter causing).
 */
public class MercenaryKillCreditMethod {

    /**
     * @return a source crediting the owner, or null when this death has nothing to do with a
     * mercenary and should be left exactly as it is.
     */
    public static DamageSource resolve(LivingEntity victim, DamageSource src) {
        try {
            // matches the guard this replaced in CommonEvents: a mercenary killing a player is not
            // the owner scoring a pvp kill.
            if (victim instanceof Player) {
                return null;
            }
            if (src == null) {
                return null;
            }

            MercenaryEntity merc = mercBehind(src.getEntity());
            if (merc == null) {
                return null;
            }
            if (!(merc.getOwner() instanceof Player owner)) {
                return null; // owner logged out, or the mercenary outlived its handle on them
            }

            // getKillCredit() -> awardKillScore (advancements, statistics) reads this, and so does
            // the lastHurtByPlayerTime > 0 "player kill" flag that vanilla loot conditions use.
            // Neither reads the damage source, so the swap below does not cover them.
            victim.setLastHurtByPlayer(owner);

            // OnMobDeathDrops needs to keep finding the mercenary so its Find stats still stack on
            // the owner's. Its damage source route stops working here - the swapped source names the
            // owner, and a mob with set_health_damage_override never set lastDamageSource in the
            // first place, since that path skips hurt() entirely and calls die() directly.
            Load.Unit(victim).lastKillCreditedMerc = merc;

            DamageSource swapped = new DamageSource(src.typeHolder(), src.getDirectEntity(), owner, src.sourcePositionRaw());
            ((DamageSourceDuck) swapped).setCreditedMerc(merc);
            return swapped;

        } catch (Exception e) {
            // a death must never throw out of here - worst case the kill just isn't credited
            e.printStackTrace();
            return null;
        }
    }

    /**
     * The mercenary responsible for this attacker: the mercenary itself, or a pet it summoned.
     * <p>
     * A mercenary's pet has the identical problem and is resolved through the same summoner chain
     * {@code PetAttackUTIL} uses to decide whose stats it swings with. A player's own summon is
     * deliberately NOT resolved here: it already attacks with the player as caster, so its source
     * is a Player to begin with, and rewriting those would start firing on-kill procs that the
     * mercenary design keeps switched off.
     */
    private static MercenaryEntity mercBehind(Entity attacker) {
        if (attacker instanceof MercenaryEntity merc) {
            return merc;
        }
        if (attacker instanceof SummonEntity summon) {
            LivingEntity summoner = Load.Unit(summon).summonedPetData.getSummoner(summon);
            if (summoner instanceof MercenaryEntity merc) {
                return merc;
            }
        }
        return null;
    }
}
