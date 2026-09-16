package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.mine_and_slash.uncommon.effectdatas.TenSecondPlayerTickEvent;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ThrottledErrors;
import com.robertx22.mine_and_slash.capability.bases.EntityGears;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.characters.PlayerStats;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenarySpellCaster;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.wizard.WizardSpellCaster;
import com.robertx22.mine_and_slash.database.data.wizard.entity.WizardEntity;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class OnEntityTick {


    public static void onTick(LivingEntity entity) {

        if (entity.level().isClientSide) {
            return;
        }

        var data = Load.Unit(entity);

        if (data == null) {
            return;
        }

        // Each subsystem below gets its OWN guard rather than sharing one try around the whole
        // method. They are unrelated to each other, and one shared catch meant a throw in, say, the
        // ailment tick also cost the entity its effect expiry, its stat recalc and its client sync
        // for that tick - and if it threw every tick, forever. That is how a cc effect's attribute
        // modifiers outlive the effect and never get cleaned up: the code that would expire it and
        // the code that would notice the leftover both live further down this method.

        // first, and deliberately before anything that can throw: reconciles the vanilla attribute
        // modifiers the cc effects are made of against the effects actually running. Cheap - it only
        // does anything for entities that have held an effect this session, and only once a second.
        if (entity.tickCount % 20 == 0) {
            guard("exile effect modifier reconcile", () -> data.getStatusEffectsData().reconcileVanillaModifiers(entity));
        }

        if (data.isSummon()) {
            guard("summoned pet tick", () -> data.summonedPetData.tick(entity));
        }

        data.immuneTicks--;

        guard("ailment tick", () -> data.ailments.onTick(entity));

        guard("status effect tick", () -> data.getStatusEffectsData().tick(entity));

        guard("cooldown tick", () -> data.getCooldowns().onTicksPass(1));

        if (entity.tickCount % 20 == 0) {
            guard("leech tick", () -> data.leech.onSecondUseLeeches(data));
        }

        // todo lets see if this works fine, no need to lag if mobs anyway recalculate stats when needed
        if (entity instanceof Player p) {
            guard("player tick", () -> {
                // a weapon swap ends whatever it was holding and buys a short lockout - a "swap
                // weapon" keybind sharing a physical key with a skill keybind was otherwise worth
                // both weapons' skills off one hotbar
                if (checkGearChanged(entity) && p instanceof ServerPlayer sp) {
                    Load.player(sp).spellCastingData.onMainHandWeaponSwapped(sp);
                }

                if (entity.tickCount % 100 == 0) {
                    for (Map.Entry<String, ResourceLocation> set : PlayerStats.REGISTERED_STATS.entrySet()) {
                        int max = Math.round(data.getUnit().getCalculatedStat(set.getKey()).getValue());
                        p.resetStat(Stats.CUSTOM.get(PlayerStats.REGISTERED_STATS.get(set.getKey())));
                        p.awardStat(Stats.CUSTOM.get(PlayerStats.REGISTERED_STATS.get(set.getKey())), max);
                    }
                }
            });
        } else if (entity instanceof MercenaryEntity merc) {
            // this branch runs for every mercenary entity, orphans included, so it is the one
            // place a duplicate can notice it isn't the one its owner is holding and remove itself
            guard("mercenary orphan check", () -> MercenaryManager.discardIfOrphan(merc));

            // mercenaries drive their own skills - they have no key inputs and no SpellCastingData,
            // and they must not pick up the mob rarity spells below.
            guard("mercenary spell tick", () -> MercenarySpellCaster.onTick(merc));

            // and their own regeneration, for the same reason: the only regen tick in the mod
            // lives in OnServerTick's player loop, so a mercenary's Health/Magic Shield Regen
            // stats were granted and then never once acted on. tickCount staggers this across
            // entities on its own.
            if (merc.tickCount % 20 == 0) {
                guard("mercenary regen", () -> MercenaryManager.tickRegen(merc));
            }

            // and the 10 second stat tick, for the same reason: TenSecondPlayerTickEvent is only
            // raised in OnServerTick's player loop, so any "every 10s, give yourself X" stat on a
            // mercenary's gear never fired. Crater's instant_traps is one - it hands out the
            // Saboteur buff the trap spells check to detonate instantly.
            if (merc.tickCount % 200 == 0) {
                guard("mercenary 10s tick", () -> new TenSecondPlayerTickEvent(merc, merc).Activate());
            }

        } else if (entity instanceof WizardEntity wizard) {
            // wizards drive their own skills, on their own interval, and must not fall through
            // to the mob rarity spells below - those fire blind with no target or range check,
            // which on a mob whose whole design is aimed casting would read as random damage
            // arriving from nowhere.
            guard("wizard spell tick", () -> WizardSpellCaster.onTick(wizard));

        } else {
            var rar = data.getMobRarity();

            if (!rar.spells.isEmpty()) {
                for (String id : rar.spells) {
                    // one bad rarity spell (a datapack id that no longer resolves) used to take the
                    // rest of this method down with it on every single tick of every mob of that
                    // rarity, so guard per spell
                    guard("mob rarity spell '" + id + "'", () -> {
                        // todo this is just a quick workaround, ideally mobs should be using the same cast code as players
                        var spell = ExileDB.Spells().get(id);

                        if (!data.getCooldowns().isOnCooldown(id)) {
                            var ctx = new SpellCastContext(entity, 0, spell);
                            spell.cast(ctx);

                            int cd = ctx.spell.getCooldownTicks(ctx);
                            ctx.data.getCooldowns().setOnCooldown(ctx.spell.GUID(), cd);
                        }
                    });
                }
            }
        }

        guard("equipment cache tick", () -> data.equipmentCache.onTick());

        guard("data sync", () -> data.sync.onTickTrySync(entity));
    }

    private static void guard(String what, Runnable run) {
        try {
            run.run();
        } catch (Exception e) {
            ThrottledErrors.log("entity_tick_" + what, "mns entity tick step failed: " + what, e);
        }
    }

    /**
     * @return whether the main hand's casting identity moved, which is a stricter question than the
     * one the stat cache asks. Stack identity is enough to know the cache is stale, but not enough to
     * punish a cast for - see {@link EntityGears#observeMainHand}.
     */
    public static boolean checkGearChanged(LivingEntity entity) {

        if (entity.level().isClientSide) {
            return false;
        }

        if (entity.isDeadOrDying()) {
            return false;
        }

        EntityData data = Load.Unit(entity);

        EntityGears gears = data.getCurrentGears();

        boolean gearChanged = false;
        boolean weaponchanged = false;
        boolean weaponSwapped = false;

        for (EquipmentSlot s : EquipmentSlot.values()) {
            ItemStack now = entity.getItemBySlot(s);
            ItemStack before = gears.get(s);

            if (now != before) {
                if (s == EquipmentSlot.MAINHAND) {
                    weaponchanged = true;
                    // only asked on the ticks the cheap reference check already flagged, because it
                    // deserializes the gear nbt
                    weaponSwapped = gears.observeMainHand(now);
                } else {
                    gearChanged = true;
                }
            }
            gears.put(s, now);
        }

        if (gearChanged) {
            data.equipmentCache.GEAR.setDirty();
        }
        if (weaponchanged) {
            data.equipmentCache.WEAPON.setDirty();
        }

        return weaponSwapped;
    }

}
