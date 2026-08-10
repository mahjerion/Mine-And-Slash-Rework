package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.mixin_methods.MapChunkRaycastGuard;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@code AbstractArrow.tick} does NOT go through {@code ProjectileUtil} - it calls
 * {@code level().clip(...)} itself - so this mod's own {@code SimpleProjectileEntity}, which extends
 * AbstractArrow, needs its own redirect to be covered.
 * <p>
 * See {@link MapChunkRaycastGuard} for why this only does anything in a map dimension.
 */
@Mixin(AbstractArrow.class)
public class AbstractArrowChunkLoadMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult mns$clipWithoutGeneratingChunks(Level level, ClipContext ctx) {
        return MapChunkRaycastGuard.clip(level, ctx);
    }
}
