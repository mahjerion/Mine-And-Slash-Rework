package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryStorageData;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenaryActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Cycles through the registered mercenary classes, the way the player picks a class. Switching
 * dismisses the current mercenary; the new one arrives after the usual delay (design section 1).
 */
public class MercClassArrowButton extends AbstractButton {

    private final MercenaryScreen screen;
    private final boolean forward;

    public MercClassArrowButton(MercenaryScreen screen, boolean forward, int x, int y) {
        super(x, y, MercGui.ARROW_SIZE, MercGui.ARROW_SIZE, Component.empty());
        this.screen = screen;
        this.forward = forward;
    }

    @Override
    public void onPress() {
        List<MercenaryClass> all = MercenaryStorageData.getAllClasses();
        if (all.size() < 2) {
            return;
        }
        String current = screen.getActiveClassId();

        int index = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).GUID().equals(current)) {
                index = i;
                break;
            }
        }
        int next = Math.floorMod(index + (forward ? 1 : -1), all.size());
        Packets.sendToServer(MercenaryActionPacket.setClass(all.get(next).GUID()));
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
        // leftright.png is a 44x44 sheet: left/right across, normal on the top row, hover below
        int u = forward ? MercGui.ARROW_SIZE : 0;
        int v = isHovered() ? MercGui.ARROW_SIZE : 0;

        gui.setColor(1F, 1F, 1F, 1F);
        gui.blit(MercGui.LEFT_RIGHT, getX(), getY(), u, v, width, height, 256, 256);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
