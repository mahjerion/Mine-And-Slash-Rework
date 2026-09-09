package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.event_hooks.player.BlockAttacksWhileCasting;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client half of "a cast owns the hands". The server already refuses the hit
 * ({@code AttackEntityEvent} in CommonEvents), but the swing is started on the client before the
 * server is asked, so it has to be refused here too or the player still sees a full swing.
 * <p>
 * Priority 1500 so this applies after Better Combat's {@code MinecraftClientInject} (default 1000):
 * that mixin merges its private {@code startUpswing} into Minecraft, and the method has to exist
 * before ours can target it. Both of Better Combat's attack paths - the click ({@code pre_doAttack})
 * and the held key ({@code pre_handleBlockBreaking}) - funnel into that one method, and it plays the
 * animation, broadcasts it and arms the attack, so cancelling it at the head stops all of it.
 */
@Mixin(value = Minecraft.class, priority = 1500)
public class BlockAttackWhileCastingMixin {

    private boolean mmorpg$isLocalPlayerCasting() {
        Minecraft mc = (Minecraft) (Object) this;
        return mc.player != null && BlockAttacksWhileCasting.isCastingClient(mc.player);
    }

    // vanilla attack, mining start and the empty-air swing all go through here
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void mmorpg$blockVanillaAttack(CallbackInfoReturnable<Boolean> cir) {
        if (mmorpg$isLocalPlayerCasting()) {
            cir.setReturnValue(false);
        }
    }

    // require = 0: silently absent when Better Combat is not installed, or if a future version
    // renames the method. the server-side cancel still holds in that case, only the swing shows
    @Dynamic("startUpswing is merged into Minecraft by Better Combat's MinecraftClientInject mixin")
    @Inject(method = "startUpswing", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mmorpg$blockBetterCombatUpswing(CallbackInfo ci) {
        if (mmorpg$isLocalPlayerCasting()) {
            ci.cancel();
        }
    }
}
