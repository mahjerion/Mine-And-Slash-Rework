package com.robertx22.mine_and_slash.capability.player.container;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.mixin_ducks.MouseHandlerDuck;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {


    public static final ResourceLocation BACKGROUND_LOCATION = new ResourceLocation(SlashRef.MODID, "textures/gui/master_bag.png");

    public static double iMouseX = (double)(Minecraft.getInstance().getWindow().getScreenWidth() / 2);
    public static double iMouseY = (double)(Minecraft.getInstance().getWindow().getHeight() / 2);

    private static final int TEXTURE_TOP_Y = 0;
    private static final int TEXTURE_TOP_HEIGHT = 115;
    private static final int TEXTURE_ROW_Y = TEXTURE_TOP_HEIGHT;
    private static final int TEXTURE_ROW_HEIGHT = 18;
    private static final int TEXTURE_BOTTOM_Y = TEXTURE_ROW_Y + TEXTURE_ROW_HEIGHT;
    private static final int TEXTURE_BOTTOM_HEIGHT = 99;

    private static final int ROW_START_Y = 16;

    protected int rows;

    public BackpackScreen(BackpackMenu pMenu, Inventory pPlayerInventory, Component txt) {
        super(pMenu, pPlayerInventory, Component.literal(""));
        this.rows = pMenu.containerRows;
        this.imageWidth = 199;
        this.imageHeight = ROW_START_Y + TEXTURE_ROW_HEIGHT * rows + TEXTURE_BOTTOM_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        int x = leftPos + 175;
        int y = topPos + 18;

        for (Backpacks.BackpackType type : Backpacks.BackpackType.values()) {
            this.addRenderableWidget(new BackpackButton(type, x, y));
            y += 18;
        }

        MouseHandlerDuck mouseHandler = (MouseHandlerDuck) Minecraft.getInstance().mouseHandler;
        //init() will be invoked when this screen be set to Minecraft.screen after the releaseMouse(), see setScreen();
        mouseHandler.setXPos(iMouseX);
        mouseHandler.setYPos(iMouseY);
        //from MouseHandler.class releaseMouse()
        InputConstants.grabOrReleaseMouse(this.minecraft.getWindow().getWindow(), 212993, iMouseX, iMouseY);
    }

    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);

    }

    @Override
    public void onClose() {
        super.onClose();
        //reset position
        iMouseX = (double)(Minecraft.getInstance().getWindow().getScreenWidth() / 2);
        iMouseY = (double)(Minecraft.getInstance().getWindow().getHeight() / 2);
    }



    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        //  pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        // pGuiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int rowY = y + ROW_START_Y;

        for (int row = 0; row < rows; row++) {
            pGuiGraphics.blit(BACKGROUND_LOCATION, x, rowY, 0, TEXTURE_ROW_Y, this.imageWidth, TEXTURE_ROW_HEIGHT);
            rowY += TEXTURE_ROW_HEIGHT;
        }

        pGuiGraphics.blit(BACKGROUND_LOCATION, x, rowY - 1, 0, TEXTURE_BOTTOM_Y, this.imageWidth, TEXTURE_BOTTOM_HEIGHT);

        // draw top border + tabs over the repeating texture
        pGuiGraphics.blit(BACKGROUND_LOCATION, x, y, 0, TEXTURE_TOP_Y, this.imageWidth, TEXTURE_TOP_HEIGHT);
    }

}
