package com.robertx22.mine_and_slash.gui.bases;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.mine_and_slash.mixin_ducks.MouseHandlerDuck;
import net.minecraft.client.Minecraft;

// Opening a server side container from a gui that already has one open makes the server close the
// old container first. That drops the client to no screen at all for a moment, which regrabs the
// mouse, and the setScreen() for the new container then releases it again and warps the cursor to
// the middle of the window.
//
// Save the cursor right before asking the server to open the menu, and put it back in the new
// screen's init(), which runs after the warp. BackpackScreen does the same thing inline for the
// backpack tabs.
public class GuiMousePosition {

    // window coordinates, and null when there is nothing worth restoring. both values always come
    // from the same coordinate space as MouseHandler's own xpos/ypos
    private static Double savedX = null;
    private static Double savedY = null;

    public static void save() {
        var mouse = Minecraft.getInstance().mouseHandler;
        savedX = mouse.xpos();
        savedY = mouse.ypos();
    }

    public static void forget() {
        savedX = null;
        savedY = null;
    }

    // consumes the saved position, so a stale one can never be applied to an unrelated screen and
    // a resize (which runs init() again) can't move the cursor a second time
    public static void restore() {

        if (savedX == null || savedY == null) {
            return;
        }

        double x = savedX;
        double y = savedY;
        forget();

        Minecraft mc = Minecraft.getInstance();

        MouseHandlerDuck mouseHandler = (MouseHandlerDuck) mc.mouseHandler;
        mouseHandler.setXPos(x);
        mouseHandler.setYPos(y);

        // the same call MouseHandler.releaseMouse() makes, 212993 is InputConstants.CURSOR_NORMAL
        InputConstants.grabOrReleaseMouse(mc.getWindow().getWindow(), 212993, x, y);
    }
}
