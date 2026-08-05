package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.event_hooks.damage_hooks.util.DmgSourceUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// when a player is both the caster and the target of our damage, it shouldn't feel like getting
// attacked. vanilla LivingEntity.hurt does four things we skip in that case: the damage broadcast
// (which makes the victim's client set hurtTime and play the hurt sound), the hurt animation packet
// (the camera tilt), the server side hurt sound, and the knockback - which for self damage has a
// zero length direction vector, so vanilla randomizes it and shoves the player somewhere random.
@Mixin(LivingEntity.class)
public abstract class SelfDamageNoTiltMixin {

    @Shadow
    protected abstract void playHurtSound(DamageSource source);

    // hurt tilt + hurt sound, both played client side from ClientboundDamageEventPacket
    @Redirect(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;broadcastDamageEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void mmorpg$noSelfDamageBroadcast(Level level, Entity entity, DamageSource src) {
        if (!DmgSourceUtils.isPlayerSelfDamage(src, (LivingEntity) (Object) this)) {
            level.broadcastDamageEvent(entity, src);
        }
    }

    // hurt tilt, ServerPlayer's override sets hurtDir and sends ClientboundHurtAnimationPacket
    @Redirect(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;indicateDamage(DD)V"))
    private void mmorpg$noSelfDamageTilt(LivingEntity self, double x, double z, DamageSource source, float amount) {
        if (!DmgSourceUtils.isPlayerSelfDamage(source, self)) {
            self.indicateDamage(x, z);
        }
    }

    @Redirect(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void mmorpg$noSelfDamageKnockback(LivingEntity self, double strength, double x, double z, DamageSource source, float amount) {
        if (!DmgSourceUtils.isPlayerSelfDamage(source, self)) {
            self.knockback(strength, x, z);
        }
    }

    // the server side copy of the hurt sound, broadcast to everyone nearby
    @Redirect(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;playHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void mmorpg$noSelfDamageHurtSound(LivingEntity self, DamageSource src) {
        if (!DmgSourceUtils.isPlayerSelfDamage(src, self)) {
            // protected in LivingEntity, so it has to go through the shadow instead of `self`.
            // the redirect receiver is `this` anyway
            this.playHurtSound(src);
        }
    }
}
