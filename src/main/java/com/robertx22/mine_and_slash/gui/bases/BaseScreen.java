package com.robertx22.mine_and_slash.gui.bases;

import com.robertx22.mine_and_slash.event_hooks.player.OnKeyPress;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.SkillTreeScreen;
import com.robertx22.mine_and_slash.gui.screens.stat_gui.StatScreen;
import com.robertx22.mine_and_slash.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.vanilla_mc.packets.proxies.OpenGuiWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BaseScreen extends Screen {

    public BaseScreen(int width, int height) {
        super(Component.literal(""));
        this.sizeX = width;
        this.sizeY = height;
    }

    public Minecraft mc = Minecraft.getInstance();

    public int guiLeft = 0;
    public int guiTop = 0;

    public int sizeX = 0;
    public int sizeY = 0;


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        // todo make this less wack
        // the two named search boxes are statics that outlive their screens, hence the explicit checks.
        // the getFocused() test covers the general case: any screen whose text field currently has focus
        // shouldn't be closed by someone typing that letter into it.
        if (KeybindsRegister.HUB_SCREEN_KEY.matches(keyCode, scanCode) && !StatScreen.SEARCH.isFocused() && !SkillTreeScreen.SEARCH.isFocused() && !(this.getFocused() instanceof EditBox)) {
            Minecraft.getInstance().setScreen(null);
            OnKeyPress.cooldown = 5;
            return false;

        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - this.sizeX) / 2;
        this.guiTop = (this.height - this.sizeY) / 2;
    }

    public <T extends AbstractWidget> T publicAddButton(T w) {
        return this.addRenderableWidget(w);
    }

    // The back button hangs above the panel's top left corner, outside the background art.
    //
    // Deliberately not called from init(): several screens clearWidgets() *after* super.init(), and
    // MercenaryScreen rebuilds its widgets from tick(), so an automatic add would be silently wiped.
    // Each screen calls this at the end of whichever method lays its widgets out.
    //
    // The y is clamped because the tallest panels (SkillGemsScreen 256, MercenaryScreen 256) leave
    // almost no room above themselves on a short window, and a button at a negative y is unclickable.
    protected BackButton addBackButton(Component tooltip, Runnable onBack) {
        return publicAddButton(new BackButton(guiLeft, Math.max(2, guiTop - BackButton.SIZE - 2), tooltip, onBack));
    }

    protected BackButton addBackToHubButton() {
        return addBackButton(Words.Main_Hub.locName(), OpenGuiWrapper::openMainHub);
    }

}
