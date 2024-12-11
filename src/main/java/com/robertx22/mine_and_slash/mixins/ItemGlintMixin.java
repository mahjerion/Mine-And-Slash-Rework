package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.mixin_methods.RenderItemGlints;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class ItemGlintMixin {


    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V", at = @At(value = "HEAD"))
    private void drawMyGlint(LivingEntity pEntity, Level pLevel, ItemStack pStack, int pX, int pY, int pSeed, int p_281995_, CallbackInfo ci) {
        try {
            GuiGraphics gui = (GuiGraphics) (Object) this;
            RenderItemGlints.drawMyGlint(gui, pStack, pX, pY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
