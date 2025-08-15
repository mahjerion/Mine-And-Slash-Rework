package com.robertx22.mine_and_slash.mixins;

import com.robertx22.ancient_obelisks.main.ObeliskMobTierStats;
import com.robertx22.ancient_obelisks.structure.ObeliskMapData;
import com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ObeliskMobTierStats.class, remap = false)
public class ObeliskMobTierStatsMixin {
    @Inject(
        method = "tryApply",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void mmorpg$tryApply(LivingEntity en, ObeliskMapData data, CallbackInfo ci) {
        if (en instanceof SummonEntity) {
            ci.cancel();
        }
    }
}
