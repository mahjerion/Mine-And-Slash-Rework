package com.robertx22.mine_and_slash.gui.screens.unique_collection;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Resets one filter category. Same cancel icon the wiki's filter rows use, so the two screens read the
 * same way.
 * <p>
 * 20x20 rather than the wiki's 30x30, to line up with the filter buttons it sits beside.
 */
public class ClearBookFilterButton extends AbstractButton {

    public static final int SIZE = 20;
    private static final int ICON = 16;

    private static final ResourceLocation TEX = SlashRef.guiId("bestiary/cancel");

    private final UniqueCollectionScreen screen;
    private final BookFilterCategory category;

    public ClearBookFilterButton(UniqueCollectionScreen screen, BookFilterCategory category, int x, int y) {
        super(x, y, SIZE, SIZE, Component.literal(""));
        this.screen = screen;
        this.category = category;
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        super.renderWidget(gui, mouseX, mouseY, delta); // vanilla button frame, then the icon on top

        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int off = (SIZE - ICON) / 2;
        // u/v of 0,0 on the 16x16 sheet. the wiki's copy passes 16,16, which is off the texture and only
        // lands on the right pixels because the sampler wraps - same result, but don't repeat it.
        gui.blit(TEX, getX() + off, getY() + off, 0, 0, ICON, ICON, ICON, ICON);
    }

    @Override
    public void onPress() {
        screen.setFilter(category, BookFilter.NONE);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
    }
}
