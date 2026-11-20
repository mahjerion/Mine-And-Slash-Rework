package com.robertx22.mine_and_slash.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;

/**
 * Quest Kill Task only credits kills to players it saw in LivingEntity.hurt. Mobs with
 * set_health_damage_override are damaged with setHealth()/die() and never reach hurt(), so a spell
 * kill on them is not credited. This tells the tracker about the attacker the same way its own
 * hurt mixin would. Soft dependency, so it goes through a method handle and does nothing without it.
 */
public class QuestKillTaskCompat {

    private static boolean resolved = false;
    private static MethodHandle trackDamage = null;

    public static void trackDamage(LivingEntity target, LivingEntity attacker) {
        try {
            if (target == null || attacker == null || target.level().isClientSide) {
                return;
            }
            ServerPlayer player = null;
            if (attacker instanceof ServerPlayer sp) {
                player = sp;
            } else if (attacker instanceof TamableAnimal pet && pet.getOwner() instanceof ServerPlayer owner) {
                player = owner; // same as the tracker's own pet handling
            }
            if (player == null) {
                return;
            }
            MethodHandle mh = handle();
            if (mh != null) {
                mh.invoke(target, player.getUUID());
            }
        } catch (Throwable e) {
            // never let a compat hook break damage
            e.printStackTrace();
        }
    }

    private static MethodHandle handle() {
        if (!resolved) {
            resolved = true;
            try {
                if (ModList.get().isLoaded("questkilltask")) {
                    Class<?> c = Class.forName("dev.muon.questkilltask.DamageTracker");
                    trackDamage = MethodHandles.publicLookup().findStatic(c, "trackDamage",
                            MethodType.methodType(void.class, LivingEntity.class, UUID.class));
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return trackDamage;
    }
}
