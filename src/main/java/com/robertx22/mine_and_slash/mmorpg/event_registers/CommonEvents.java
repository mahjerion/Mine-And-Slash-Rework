package com.robertx22.mine_and_slash.mmorpg.event_registers;

import com.robertx22.mine_and_slash.maps.MapEntryTickets;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.database.DatabaseCaches;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.LivingHurtUtils;
import com.robertx22.mine_and_slash.mixin_ducks.DamageSourceDuck;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.reworked.NewDamageMain;
import com.robertx22.mine_and_slash.event_hooks.entity.OnMobSpawn;
import com.robertx22.mine_and_slash.event_hooks.entity.OnTrackEntity;
import com.robertx22.mine_and_slash.event_hooks.my_events.OnEntityTick;
import com.robertx22.mine_and_slash.event_hooks.my_events.OnLootChestEvent;
import com.robertx22.mine_and_slash.event_hooks.my_events.OnMobDeathDrops;
import com.robertx22.mine_and_slash.event_hooks.my_events.OnPlayerDeath;
import com.robertx22.mine_and_slash.event_hooks.ontick.OnServerTick;
import com.robertx22.mine_and_slash.event_hooks.player.OnLogin;
import com.robertx22.mine_and_slash.event_hooks.player.StopCastingIfInteract;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mixin_methods.OnItemInteract;
import com.robertx22.mine_and_slash.mmorpg.ForgeEvents;
import com.robertx22.mine_and_slash.mmorpg.ModErrors;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashEntities;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashPotions;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.OnDeathEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.OnMobKilledByDamageEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.Arrays;

public class CommonEvents {

    public static void register() {

        ForgeEvents.registerForgeEvent(EntityAttributeCreationEvent.class, x -> {
            x.put(SlashEntities.SPIRIT_WOLF.get(), Wolf.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.6).add(Attributes.MAX_HEALTH, 20).build());
            x.put(SlashEntities.SKELETON.get(), Skeleton.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.4).add(Attributes.MAX_HEALTH, 10).build());
            x.put(SlashEntities.SPIDER.get(), Spider.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.MAX_HEALTH, 5).build());
            x.put(SlashEntities.ZOMBIE.get(), Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.MAX_HEALTH, 15).build());

            x.put(SlashEntities.FIRE_GOLEM.get(), Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.MAX_HEALTH, 15).build());
            x.put(SlashEntities.COLD_GOLEM.get(), Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.MAX_HEALTH, 15).build());
            x.put(SlashEntities.LIGHTNING_GOLEM.get(), Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.MAX_HEALTH, 15).build());

            // max health here is only a seed - the mercenary's real pool comes from its Health stat,
            // recalculated from its class profile and gear. speed matches the player's walk so it
            // can actually keep up while following.
            x.put(SlashEntities.MERCENARY.get(), Zombie.createAttributes()
                    .add(Attributes.MOVEMENT_SPEED, 0.3)
                    .add(Attributes.MAX_HEALTH, 20)
                    .add(Attributes.ATTACK_DAMAGE, 1)
                    .add(Attributes.FOLLOW_RANGE, 32)
                    .build());

            // wizards. max health is a seed only - the real pool comes from the Health stat, rolled
            // off the map's tier like any other monster. FOLLOW_RANGE is doing real work though:
            // WizardSpellCaster.engageRange returns it for a skill with no distance to close, and
            // clamps every other skill's computed reach to it, so this is the wizard's true maximum
            // engagement distance. Speed is under a player's walk, so kiting buys it seconds rather
            // than letting it outrun anyone forever.
            for (var wizard : Arrays.asList(SlashEntities.FIRE_WIZARD, SlashEntities.ICE_WIZARD,
                    SlashEntities.LIGHTNING_WIZARD, SlashEntities.CHAOS_WIZARD)) {
                x.put(wizard.get(), Monster.createMonsterAttributes()
                        .add(Attributes.MOVEMENT_SPEED, 0.26)
                        .add(Attributes.MAX_HEALTH, 26)
                        .add(Attributes.FOLLOW_RANGE, 24)
                        .build());
            }

        });


        OnItemInteract.register();


        // instant bows
        ForgeEvents.registerForgeEvent(ArrowLooseEvent.class, event -> {
            if (event.getEntity().hasEffect(SlashPotions.INSTANT_ARROWS.get())) {
                event.setCharge(100);
            }
        });
        ForgeEvents.registerForgeEvent(ArrowNockEvent.class, event -> {
            if (event.getEntity().hasEffect(SlashPotions.INSTANT_ARROWS.get())) {
                event.setAction(InteractionResultHolder.pass(event.getBow()));
                event.getBow().releaseUsing(event.getLevel(), event.getEntity(), 2000);
                int cd = 20 - event.getEntity().getEffect(SlashPotions.INSTANT_ARROWS.get()).getAmplifier();

                if (cd < 3) {
                    cd = 3;
                }
                event.getEntity().getCooldowns().addCooldown(event.getBow().getItem(), cd); // todo
            }
        });
        ForgeEvents.registerForgeEvent(TickEvent.PlayerTickEvent.class, event -> {
            if (!event.player.level().isClientSide) {
                if (event.player.hasEffect(SlashPotions.INSTANT_ARROWS.get())) {
                    if (event.player.getMainHandItem().getItem() instanceof BowItem) {
                        event.player.getMainHandItem().getOrCreateTag().putBoolean("instant", true);
                    }
                }
            }
        });


        // instant bows


        ForgeEvents.registerForgeEvent(LivingDeathEvent.class, event -> {

            if (event.getEntity() != null) {

                // the mercenary itself died. it keeps its experience (design section 5) and comes back
                // once its owner is out of combat.
                if (event.getEntity() instanceof MercenaryEntity deadMerc && deadMerc.getOwner() instanceof Player owner) {
                    // its pets die with it, the same as on a dismiss. they are owned by the player,
                    // so no owner side despawn path can see them as the mercenary's.
                    MercenaryManager.despawnSummons(deadMerc);
                    Load.player(owner).mercs.spawnedId = null;
                    MercenaryManager.requestRespawn(owner);
                }

                // the getCreditedMerc() test is the mercenary carve-out. MercenaryKillCreditMixin has
                // already rewritten the source of a mercenary's kill to name the owner as the causing
                // entity, so that a merc kill counts for loot tables, advancements and quest mods -
                // but the design wants participation only, never on-kill stats, and without this the
                // rewrite would start firing them. Set on that substitute source and nothing else.
                if (event.getSource().getEntity() instanceof Player p
                        && ((DamageSourceDuck) event.getSource()).getCreditedMerc() == null) {
                    LivingEntity target = event.getEntity();
                    if (!Load.Unit(target).getCooldowns().isOnCooldown("onkill")) {
                        DamageEvent dmg = Load.Unit(target).lastDamageTaken;
                        if (dmg != null) {
                            // make absolutely sure this isn't called twice somehow
                            Load.Unit(target).getCooldowns().setOnCooldown("onkill", Integer.MAX_VALUE);
                            OnMobKilledByDamageEvent e = new OnMobKilledByDamageEvent(dmg);
                            e.Activate();
                        }
                    }
                }

                LivingEntity deadMob = event.getEntity();
                Entity test = event.getSource().getEntity();
                LivingEntity killer = null;
                if (test instanceof LivingEntity en) {
                    killer = en;
                } else {
                    killer = deadMob;
                }
                if (!Load.Unit(deadMob).getCooldowns().isOnCooldown(OnDeathEvent.ID)) {
                    // make absolutely sure this isn't called twice somehow
                    Load.Unit(deadMob).getCooldowns().setOnCooldown(OnDeathEvent.ID, Integer.MAX_VALUE);
                    OnDeathEvent e = new OnDeathEvent(deadMob, deadMob, event.getSource());
                    e.Activate();

                    if (deadMob instanceof SummonEntity summonEntity) {
                        Load.Unit(summonEntity).summonedPetData.onDeath(summonEntity);
                    }
                }
            }
        });


        ForgeEvents.registerForgeEvent(EntityJoinLevelEvent.class, event ->
        {
            try {
                if (event.getEntity() == null) {
                    return;
                }

                if (event.getEntity() instanceof LivingEntity en) {
                    Load.Unit(en).equipmentCache.setAllDirty(); // todo this is a new performance test
                    // does NOT saving stats to nbt, but calculating every time entity joins world make servers better or worse off?
                }
                OnMobSpawn.onLoad(event.getEntity());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        ForgeEvents.registerForgeEvent(EntityItemPickupEvent.class, event ->

        {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (!player.level().isClientSide) {
                    ItemEntity item = event.getItem();
                    ItemStack stack = item.getItem();
                    if (!stack.isEmpty()) {

                        if (!player.level().isClientSide) {
                            if (Load.player(player).config.salvage.trySalvageOnPickup(player, stack)) {
                                stack.shrink(100);
                            } else {
                                ExileStack ex = new ExileStack();
                                ex.setStack(stack); // we need to write to the stack directly instead of copying
                                ex.get(StackKeys.DROPPED).delete(); // clear dropped item data when we pick up
                                Load.backpacks(player).getBackpacks().tryAutoPickup(event.getEntity(), stack);
                            }
                        }
                    }
                }
            }
        }, EventPriority.HIGHEST);

        ForgeEvents.registerForgeEvent(PlayerEvent.Clone.class, event ->

        {
            try {
                if (event.getEntity() instanceof ServerPlayer p) {
                    if (!p.level().isClientSide) {
                        var data = Load.player(p);
                        data.spellCastingData.cancelCast(p); // so player doesn't continue casting spell after reviving
                    }
                }
            } catch (Exception e) {
                ModErrors.print(e);
            }
        });

        // LOWEST so this lands after Library-of-Exile's own Clone handler (ApiForgeEvents ->
        // PlayerCapabilities.saveAllOnDeath), which rebuilds PlayerData through serializeNBT /
        // deserializeNBT. mercs.spawnedId is transient - right for the world save, since the entity
        // itself is never written there - but that same round trip is what a respawn goes through,
        // so the new player came out with no handle on the mercenary still standing in the world.
        // getMerc() and dismiss() both resolve through that id, so the old one became an orphan
        // nothing could ever find and the tick spawned a second. Fires for the End portal too, where
        // the player entity is likewise rebuilt without a death.
        ForgeEvents.registerForgeEvent(PlayerEvent.Clone.class, event ->

        {
            try {
                event.getOriginal().reviveCaps();

                var from = Load.player(event.getOriginal()).mercs;
                var to = Load.player(event.getEntity()).mercs;

                to.spawnedId = from.spawnedId;
                // carried too, so a mercenary that died just before its owner keeps its countdown
                // instead of restarting it. blockedTicks is diagnostic only and may reset.
                to.respawnTimer = from.respawnTimer;
            } catch (Exception e) {
                ModErrors.print(e);
            }
        }, EventPriority.LOWEST);

        // the mercenary is never saved to the world, so it is only ever brought back from the owner's
        // data (OnLogin -> MercenaryManager.requestRespawn). nothing removed it when that owner LEFT
        // though: with getOwner() null it stopped fighting and following, but stood in the chunk until
        // it unloaded - hours, on a busy server - and a relog spawned a second one beside it.
        // PlayerLoggedOutEvent fires at the top of PlayerList.remove, before the player entity leaves
        // the level, so caps and level are still live and dismiss() can find it through spawnedId.
        ForgeEvents.registerForgeEvent(PlayerEvent.PlayerLoggedOutEvent.class, event ->

        {
            try {
                if (event.getEntity() instanceof ServerPlayer p) {
                    MercenaryManager.dismiss(p);
                }
            } catch (Exception e) {
                ModErrors.print(e);
            }
        });


        ForgeEvents.registerForgeEvent(TickEvent.PlayerTickEvent.class, event ->

        {
            if (!event.player.level().isClientSide) {
                if (event.phase == TickEvent.Phase.END) {
                    OnServerTick.onEndTick((ServerPlayer) event.player);
                }
            }
        });


        ForgeEvents.registerForgeEvent(AttackEntityEvent.class, event ->

        {
            if (event.getEntity() instanceof ServerPlayer) {
                StopCastingIfInteract.interact(event.getEntity());
            }
        });

        ForgeEvents.registerForgeEvent(PlayerEvent.StartTracking.class, event ->

        {
            if (event.getEntity() instanceof ServerPlayer) {
                OnTrackEntity.onPlayerStartTracking((ServerPlayer) event.getEntity(), event.getTarget());
            }
        });

        ForgeEvents.registerForgeEvent(PlayerEvent.PlayerRespawnEvent.class, event ->

        {
            if (event.getEntity() instanceof ServerPlayer) {
                Load.Unit(event.getEntity()).setAllDirtyOnLoginEtc();
            }
        });

        ForgeEvents.registerForgeEvent(PlayerEvent.PlayerChangedDimensionEvent.class, event ->

        {
            if (event.getEntity() instanceof ServerPlayer p) {
                // the client rebuilds the player entity and its capabilities on a dimension change,
                // so the client's PlayerData is blank until we push it again
                Load.player(p).forceNextSync();

                // Entry Tickets. This is the one hook that sees EVERY arrival into a map dimension,
                // including teleports from other mods that never touch the map device - see
                // MapEntryTickets.onArrival for why the charge happens here and not at the button.
                MapEntryTickets.onArrival(p, event.getFrom().location());
            }
        });

        ForgeEvents.registerForgeEvent(LivingEvent.LivingTickEvent.class, event ->

        {
            OnEntityTick.onTick(event.getEntity());
        });

        ExileEvents.ON_CHEST_LOOTED.register(new OnLootChestEvent());
        ExileEvents.MOB_DEATH.register(new OnMobDeathDrops());

        NewDamageMain.init();


        // ExileEvents.DAMAGE_BEFORE_CALC.register(new ScaleVanillaMobDamage()); todo this doesnt seem needed..?
        //ExileEvents.DAMAGE_BEFORE_CALC.register(new ScaleVanillaPlayerDamage()); todo same


        ExileEvents.PLAYER_DEATH.register(new OnPlayerDeath());


        ForgeEvents.registerForgeEvent(LivingDamageEvent.class, event ->

        {
            try {
                if (event.getEntity() instanceof Player) {
                    if (LivingHurtUtils.isEnviromentalDmg(event.getSource())) {
                        // spend magic shield on envi dmg
                        float dmg = event.getAmount();
                        float multi = dmg / event.getEntity().getMaxHealth();
                        float spend = Load.Unit(event.getEntity()).getUnit().magicShieldData().getValue() * multi;
                        Load.Unit(event.getEntity()).getResources().spend(event.getEntity(), ResourceType.magic_shield, spend);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        });


        ExileEvents.ON_PLAYER_LOGIN.register(new EventConsumer<ExileEvents.OnPlayerLogin>() {
            @Override
            public void accept(ExileEvents.OnPlayerLogin event) {
                OnLogin.onLoad(event.player);
            }
        });


        DatabaseCaches.init();

    }


}
