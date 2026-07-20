package com.robertx22.mine_and_slash.gui.screens.atlas_map;

import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// Small nav button that switches between the two Atlas screens. Uses its own dedicated texture
// (drop the PNG at src/main/resources/assets/mmorpg/textures/gui/atlas_map/button.png - a
// WIDTH x (HEIGHT*2) sheet, idle frame on top / hover frame on bottom, the same 2-frame
// ImageButton convention already used elsewhere, e.g. MainHubButton) instead of reusing the
// hub's icon-overlay button, which doesn't fit this screen's positioning/sizing needs.
public class AtlasNavButton extends ImageButton {

    public static final int WIDTH = 79;
    public static final int HEIGHT = 21;
    private static final ResourceLocation TEXTURE = SlashRef.guiId("atlas_map/button");

    private final Component label;

    // label shows which screen you're currently on; clicking navigates to target
    public AtlasNavButton(int x, int y, INamedScreen current, INamedScreen target) {
        super(x, y, WIDTH, HEIGHT, 0, 0, HEIGHT, TEXTURE, (button) -> Minecraft.getInstance().setScreen((Screen) target));
        this.label = current.screenName().locName();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, label, getX() + width / 2, getY() + (height - font.lineHeight) / 2, 0xFFFFFFFF);
    }
}
