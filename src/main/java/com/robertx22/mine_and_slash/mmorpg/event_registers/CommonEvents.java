package com.robertx22.mine_and_slash.mmorpg.event_registers;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.database.DatabaseCaches;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.LivingHurtUtils;
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

public class CommonEvents {

    public static void register() {

        ForgeEvents.registerForgeEvent(EntityAttributeCreationEvent.class, x -> {
            x.put(SlashEntities.SPIRIT_WOLF.get(), Wolf.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.4).build());
            x.put(SlashEntities.SKELETON.get(), Skeleton.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).build());
            x.put(SlashEntities.SPIDER.get(), Spider.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.5).build());
            x.put(SlashEntities.ZOMBIE.get(), Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.4).build());

            x.put(SlashEntities.FIRE_GOLEM.get(), Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.45).build());
            x.put(SlashEntities.COLD_GOLEM.get(), Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.45).build());
            x.put(SlashEntities.LIGHTNING_GOLEM.get(), Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.45).build());

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
                if (event.getSource().getEntity() instanceof Player p) {
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
