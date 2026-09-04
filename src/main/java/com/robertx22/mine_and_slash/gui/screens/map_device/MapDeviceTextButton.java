package com.robertx22.mine_and_slash.gui.screens.map_device;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A labelled button on the Atlas nav button's 79x21 sheet. Drawn by hand rather than through
 * {@code ImageButton} because that class picks a third frame two rows down for a disabled button, and
 * the sheet only has the two - a disabled "Initiate Map" would simply vanish.
 */
public class MapDeviceTextButton extends AbstractButton {

    private final Runnable onPress;

    public MapDeviceTextButton(int x, int y, Component label, Runnable onPress) {
        super(x, y, MapDeviceGui.BUTTON_W, MapDeviceGui.BUTTON_H, label);
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        gui.setColor(1F, 1F, 1F, 1F);
        int v = this.active && this.isHoveredOrFocused() ? MapDeviceGui.BUTTON_H : 0;
        gui.blit(MapDeviceGui.BUTTON, getX(), getY(), 0, v, width, height, MapDeviceGui.BUTTON_TEX_SIZE, MapDeviceGui.BUTTON_TEX_SIZE);

        var font = Minecraft.getInstance().font;
        Component label = this.active ? getMessage() : getMessage().copy().withStyle(ChatFormatting.DARK_GRAY);
        gui.drawCenteredString(font, label, getX() + width / 2, getY() + (height - font.lineHeight) / 2, 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
