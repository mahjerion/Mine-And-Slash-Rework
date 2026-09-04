package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.uncommon.utilityclasses.EnlightenMarkup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Removes Enlighten glossary markup ({@code [text](term)}) from text drawn outside tooltips, see
 * {@link EnlightenMarkup}. These two overloads are the ones everything funnels into in 1.20.1: the
 * Component overload and drawCenteredString convert to a FormattedCharSequence first, drawWordWrap
 * splits into FormattedCharSequences, and the raw String overload draws directly.
 */
@Mixin(GuiGraphics.class)
public class GuiGraphicsStripMarkupMixin {

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence mmorpg$stripMarkup(FormattedCharSequence text) {
        return EnlightenMarkup.strip(text);
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", at = @At("HEAD"), argsOnly = true)
    private String mmorpg$stripMarkup(String text) {
        return EnlightenMarkup.strip(text);
    }
}
