package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.mixin_methods.MapChunkRaycastGuard;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Every projectile's per tick "did I hit something" raycast goes through the private
 * {@code ProjectileUtil.getHitResult}, which both {@code getHitResultOnMoveVector} and
 * {@code getHitResultOnViewVector} delegate to. Redirecting the one clip call inside it therefore
 * covers every projectile in the game - including other mods' {@code ThrowableProjectile}s, which is
 * what the two watchdog crashes were actually ticking.
 * <p>
 * See {@link MapChunkRaycastGuard} for why this only does anything in a map dimension.
 */
@Mixin(ProjectileUtil.class)
public class ProjectileRaycastChunkLoadMixin {

    @Redirect(method = "getHitResult", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private static BlockHitResult mns$clipWithoutGeneratingChunks(Level level, ClipContext ctx) {
        return MapChunkRaycastGuard.clip(level, ctx);
    }
}
