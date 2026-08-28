package com.robertx22.mine_and_slash.event_hooks.my_events;

import com.robertx22.mine_and_slash.capability.bases.EntityGears;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.characters.PlayerStats;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenarySpellCaster;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class OnEntityTick {


    public static void onTick(LivingEntity entity) {

        try {

            if (entity.level().isClientSide) {
                return;
            }

            var data = Load.Unit(entity);

            if (data == null) {
                return;
            }

            if (data.isSummon()) {
                data.summonedPetData.tick(entity);
            }

            data.immuneTicks--;


            data.ailments.onTick(entity);

            data.getStatusEffectsData().tick(entity);

            data.getCooldowns().onTicksPass(1);

            if (entity.tickCount % 20 == 0) {
                data.leech.onSecondUseLeeches(data);
            }


            // todo lets see if this works fine, no need to lag if mobs anyway recalculate stats when needed
            if (entity instanceof Player) {
                checkGearChanged(entity);

                if (entity.tickCount % 100 == 0) {
                    Player p = (Player) entity;
                    for (Map.Entry<String, ResourceLocation> set : PlayerStats.REGISTERED_STATS.entrySet()) {
                        int max = Math.round(data.getUnit().getCalculatedStat(set.getKey()).getValue());
                        p.resetStat(Stats.CUSTOM.get(PlayerStats.REGISTERED_STATS.get(set.getKey())));
                        p.awardStat(Stats.CUSTOM.get(PlayerStats.REGISTERED_STATS.get(set.getKey())), max);
                    }
                }
            } else if (entity instanceof MercenaryEntity merc) {
                // this branch runs for every mercenary entity, orphans included, so it is the one
                // place a duplicate can notice it isn't the one its owner is holding and remove itself
                MercenaryManager.discardIfOrphan(merc);

                // mercenaries drive their own skills - they have no key inputs and no SpellCastingData,
                // and they must not pick up the mob rarity spells below.
                MercenarySpellCaster.onTick(merc);

                // and their own regeneration, for the same reason: the only regen tick in the mod
                // lives in OnServerTick's player loop, so a mercenary's Health/Magic Shield Regen
                // stats were granted and then never once acted on. tickCount staggers this across
                // entities on its own.
                if (merc.tickCount % 20 == 0) {
                    MercenaryManager.tickRegen(merc);
                }

            } else {

                var rar = Load.Unit(entity).getMobRarity();

                if (!rar.spells.isEmpty()) {
                    for (String id : rar.spells) {

                        // todo this is just a quick workaround, ideally mobs should be using the same cast code as players
                        var spell = ExileDB.Spells().get(id);

                        if (!data.getCooldowns().isOnCooldown(id)) {
                            var ctx = new SpellCastContext(entity, 0, spell);
                            spell.cast(ctx);

                            int cd = ctx.spell.getCooldownTicks(ctx);
                            ctx.data.getCooldowns().setOnCooldown(ctx.spell.GUID(), cd);
                        }
                    }

                }

            }

            data.equipmentCache.onTick();

            data.sync.onTickTrySync(entity);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void checkGearChanged(LivingEntity entity) {

        if (entity.level().isClientSide) {
            return;
        }

        if (entity.isDeadOrDying()) {
            return;
        }

        EntityData data = Load.Unit(entity);

        EntityGears gears = data.getCurrentGears();

        boolean gearChanged = false;
        boolean weaponchanged = false;

        for (EquipmentSlot s : EquipmentSlot.values()) {
            ItemStack now = entity.getItemBySlot(s);
            ItemStack before = gears.get(s);

            if (now != before) {
                if (s == EquipmentSlot.MAINHAND) {
                    weaponchanged = true;
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

    }

}
