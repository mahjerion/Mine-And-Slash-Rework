package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.mixin_methods.MercenaryKillCreditMethod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Rewrites the death's damage source so a mercenary's kill is credited to its owner. See
// MercenaryKillCreditMethod for why, and for why this is safe for aggro and Find stats.
//
// HEAD is load bearing: Forge's onLivingDeath (LivingDeathEvent) is the FIRST statement of die(),
// and getKillCredit(), awardKillScore() and dropAllDeathLoot() all come after it. Injecting here
// means one swap covers advancements, quest mods, LivingDropsEvent and the loot table context.
@Mixin(LivingEntity.class)
public class MercenaryKillCreditMixin {

    @ModifyVariable(method = "die", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    public DamageSource creditMercKillToOwner(DamageSource src) {
        LivingEntity victim = (LivingEntity) (Object) this;
        DamageSource swapped = MercenaryKillCreditMethod.resolve(victim, src);
        return swapped == null ? src : swapped;
    }
}
