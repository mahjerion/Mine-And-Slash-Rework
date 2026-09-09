package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.uncommon.utilityclasses.EnlightenMarkup;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Removes Enlighten glossary markup ({@code [text](term)}) from every piece of text the font draws, see
 * {@link EnlightenMarkup}. Hooking {@link Font} rather than {@code GuiGraphics.drawString} catches the
 * callers that never go through GuiGraphics: chat mods that render lines themselves (ChatPlus), tooltip
 * image components that draw their own text ({@code SocketTooltip}), HUD overlays, name tags.
 * <p>
 * In 1.20.1 every {@code drawInBatch} overload funnels into the two hooked here: the 10-arg String
 * overload adds the bidi flag and calls the 11-arg one, the Component overload converts to a
 * FormattedCharSequence first. The width overloads are hooked too so centred text and tooltip boxes
 * are measured on what is actually drawn.
 */
@Mixin(Font.class)
public class FontStripMarkupMixin {

    @ModifyVariable(method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I", at = @At("HEAD"), argsOnly = true)
    private String mmorpg$stripMarkupDraw(String text) {
        return EnlightenMarkup.strip(text);
    }

    @ModifyVariable(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence mmorpg$stripMarkupDraw(FormattedCharSequence text) {
        return EnlightenMarkup.strip(text);
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String mmorpg$stripMarkupWidth(String text) {
        return EnlightenMarkup.strip(text);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence mmorpg$stripMarkupWidth(FormattedCharSequence text) {
        return EnlightenMarkup.strip(text);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedText mmorpg$stripMarkupWidth(FormattedText text) {
        return EnlightenMarkup.strip(text);
    }
}
