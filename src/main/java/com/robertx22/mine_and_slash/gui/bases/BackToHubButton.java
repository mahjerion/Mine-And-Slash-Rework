package com.robertx22.mine_and_slash.gui.bases;

import com.robertx22.mine_and_slash.gui.screens.character_screen.MainHubScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.ResourceLocation;

// Returns the player to the Main Hub. Lives in gui/bases because nothing about it is specific to any
// one screen - any screen that wants a "back" affordance can drop one in.
//
// The texture is a 256x256 sheet whose sprite is 26x16 at (0,0) with the hover frame directly below
// it at (0,16), which is the standard 2-frame ImageButton convention already used by AtlasNavButton
// and MainHubButton - so yDiffTex = HEIGHT gives the hover swap for free.
public class BackToHubButton extends ImageButton {

    public static final int WIDTH = 26;
    public static final int HEIGHT = 16;

    private static final ResourceLocation TEXTURE = SlashRef.guiId("back_button");

    public BackToHubButton(int x, int y) {
        super(x, y, WIDTH, HEIGHT, 0, 0, HEIGHT, TEXTURE,
                b -> Minecraft.getInstance().setScreen(new MainHubScreen()));
        // Words.Character is what MainHubScreen.screenName() returns, so this needs no new
        // localization entry
        setTooltip(Tooltip.create(Words.Main_Hub.locName()));
    }
}
