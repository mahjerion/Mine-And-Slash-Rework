package com.robertx22.mine_and_slash.gui.screens.stat_gui;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.gui.components.ImageButton;

public class StatDirectionNavigationButton extends ImageButton {
    public static int xSize = 22;
    public static int ySize = 22;

    public StatDirectionNavigationButton(StatScreen screen, int x, int y, int pagination, boolean scrollDown) {
        super(x, y, xSize, ySize, scrollDown ? 44 : 66, 0, SlashRef.guiId("leftright/leftright"), (button) -> {
            screen.moveCurrentElementBy(scrollDown ? pagination : -pagination);
        });
    }
}
