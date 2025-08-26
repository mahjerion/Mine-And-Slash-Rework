package com.robertx22.mine_and_slash.mixins;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.robertx22.mine_and_slash.itemstack.ExileStack;

@Mixin(GuiGraphics.class)
public class ItemMirrorMixin {
    @Inject(
        method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void adjustScale(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int guiOffset, CallbackInfo ci) {
        if (ExileStack.of(stack).isMirrored()) {
            ((GuiGraphics) (Object) this).pose().scale(-1F, 1F, 1F);
            // we reverse the winding order by mirroring the model
            GL11.glFrontFace(GL11.GL_CW);
        }
    }

    @Inject(
        method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
        at = @At(value = "RETURN"))
    private void restoreFrontFace(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int guiOffset, CallbackInfo ci) {
        GL11.glFrontFace(GL11.GL_CCW);
    }
}