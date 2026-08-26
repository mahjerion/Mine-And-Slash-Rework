package com.robertx22.mine_and_slash.gui.inv_gui;

import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.vanilla_mc.packets.proxies.OpenGuiWrapper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class InvGuiScreen extends BaseScreen {
    static ResourceLocation TEX = SlashRef.guiId("inv_gui/background");

    InvGuiGrid grid;

    // This screen is the generic item picker - it backs the "Features" grid and the salvage config off
    // the hub, but also the skill gem spell picker and the mercenary equip pickers, which are reached
    // from another screen. A hardcoded hub target would strand those, so the destination is passed in.
    private final Component backName;
    private final Runnable onBack;

    public InvGuiScreen(InvGuiGrid grid) {
        this(grid, Words.Main_Hub.locName(), OpenGuiWrapper::openMainHub);
    }

    public InvGuiScreen(InvGuiGrid grid, Component backName, Runnable onBack) {
        super(177, 136);

        this.grid = grid;
        this.backName = backName;
        this.onBack = onBack;
    }


    @Override
    public void render(GuiGraphics gui, int x, int y, float ticks) {


        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        gui.blit(TEX, mc.getWindow()
                        .getGuiScaledWidth() / 2 - sizeX / 2,
                mc.getWindow()
                        .getGuiScaledHeight() / 2 - sizeY / 2, 0, 0, sizeX, sizeY
        );

        super.render(gui, x, y, ticks);


    }

    @Override
    protected void init() {
        super.init();
        int i = 0;
        for (int y = 0; y < InvGuiGrid.Y_MAX; y++) {
            for (int x = 0; x < InvGuiGrid.X_MAX; x++) {

                int xpos = guiLeft + 8 + (x * InvGuiButton.BUTTON_SIZE_X);
                int ypos = guiTop + 17 + (y * InvGuiButton.BUTTON_SIZE_Y);

                this.publicAddButton(new InvGuiButton(grid.list.get(i), xpos, ypos));

                i++;
            }
        }

        addBackButton(backName, onBack);
    }
}
