package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.mixin_ducks.ProjectileEntityDuck;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public class ArrowShootMixin implements ProjectileEntityDuck {

    // 1, not 0: a draw we never measured counts as a full draw. Only shootFromRotation records one,
    // so a bolt loosed through shoot() while the main hand holds a bow, an arrow rebuilt from NBT
    // after a chunk reload, or a modded bow that skips shootFromRotation would otherwise be
    // multiplied by 0 in DamageEvent.modifyIfArrowDamage and deal nothing
    public float dmg$multi = 1;

    @Inject(method = "shootFromRotation", at = @At("HEAD"))
    public void myOnShoot(Entity user, float pitch, float yaw, float roll, float modifierZ, float modifierXYZ, CallbackInfo ci) {
        Projectile arrow = (Projectile) (Object) this;
        dmg$multi = Mth.clamp(modifierZ / 3F, 0, 1); // by default it's multiplied by 3 so i need to divide it
    }

    @Override
    public float my$getDmgMulti() {
        return dmg$multi;
    }
}


