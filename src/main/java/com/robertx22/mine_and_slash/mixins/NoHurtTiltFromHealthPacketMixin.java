package com.robertx22.mine_and_slash.mixins;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// second half of the self damage fix, see SelfDamageNoTiltMixin for the server side.
//
// LocalPlayer.hurtTo starts a hurt animation whenever ClientboundSetHealthPacket reports less
// health than the client had. it knows nothing about what caused the loss, so it tilts the camera
// even when the server deliberately skipped both damage packets. that health packet is unavoidable
// - the damage really did happen - so the tilt has to be stopped here instead.
//
// a real hit always sends ClientboundDamageEventPacket (or the hurt animation packet) during
// hurt(), while the health update only goes out at the end of the server tick, so by the time
// hurtTo runs for a genuine attack hurtTime is already set with a proper hurt direction. that
// makes the health packet's own tilt purely a fallback, and we only cancel it when it would be
// *starting* a tilt rather than refreshing one that a damage packet already began.
//
// side effect: health lost without any damage packet (another mod calling setHealth, or damage
// taken during invulnerability frames) no longer flashes the screen.
@Mixin(LocalPlayer.class)
public abstract class NoHurtTiltFromHealthPacketMixin {

    @Unique
    private int mmorpg$hurtTimeBeforeHealthUpdate;

    @Inject(method = "hurtTo", at = @At("HEAD"))
    private void mmorpg$rememberHurtTime(float health, CallbackInfo ci) {
        mmorpg$hurtTimeBeforeHealthUpdate = ((LocalPlayer) (Object) this).hurtTime;
    }

    @Inject(method = "hurtTo", at = @At("TAIL"))
    private void mmorpg$dontStartTiltFromHealthUpdate(float health, CallbackInfo ci) {
        if (mmorpg$hurtTimeBeforeHealthUpdate <= 0) {
            LocalPlayer self = (LocalPlayer) (Object) this;
            self.hurtTime = 0;
            self.hurtDuration = 0;
        }
    }
}
