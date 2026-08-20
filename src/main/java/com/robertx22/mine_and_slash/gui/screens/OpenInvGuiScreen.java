package com.robertx22.mine_and_slash.gui.screens;

import com.robertx22.mine_and_slash.gui.bases.IContainerNamedScreen;
import com.robertx22.mine_and_slash.gui.inv_gui.InvGuiGrid;
import com.robertx22.mine_and_slash.gui.inv_gui.InvGuiScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class OpenInvGuiScreen implements IContainerNamedScreen {
    Words word;
    String icon;
    InvGuiGrid grid;
    // set when the screen is a subclass that adds its own widgets, and the plain grid isn't enough
    Supplier<Screen> screen;

    public OpenInvGuiScreen(Words word, String icon, InvGuiGrid grid) {
        this.word = word;
        this.icon = icon;
        this.grid = grid;
    }

    public OpenInvGuiScreen(Words word, String icon, Supplier<Screen> screen) {
        this.word = word;
        this.icon = icon;
        this.screen = screen;
    }

    @Override
    public ResourceLocation iconLocation() {
        return new ResourceLocation(SlashRef.MODID, "textures/gui/main_hub/icons/" + icon + ".png");
    }

    @Override
    public Words screenName() {
        return word;
    }

    @Override
    public void openContainer() {
        Minecraft.getInstance().setScreen(screen == null ? new InvGuiScreen(grid) : screen.get());
    }
}
