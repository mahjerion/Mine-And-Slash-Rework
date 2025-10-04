package com.robertx22.mine_and_slash.capability.player.container;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class JewelScreen extends AbstractContainerScreen<JewelsMenu> {

    private static final ResourceLocation COMPONENT = SlashRef.guiId("jewel/jewel_component");


    public JewelScreen(JewelsMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(leftPos, topPos, 0);
        guiGraphics.blit(COMPONENT, 0, 0, 0, 0, 176, 204);
        //guiGraphics.drawString(Minecraft.getInstance().font, "1", center.x, center.y, ChatFormatting.BLACK.getColor());
        if (menu.getPositions().isEmpty()){
            String string = Words.NO_JEWEL_SLOT.locName().getString();
            guiGraphics.drawString(Minecraft.getInstance().font, string, JewelsMenu.center.x - Minecraft.getInstance().font.width(string) / 2, JewelsMenu.center.y - Minecraft.getInstance().font.lineHeight / 2, ChatFormatting.BLACK.getColor(),false);
        } else {
            for (Vector2i position : menu.getPositions()) {
                //idk why I need -1 here but it just can't fit in if I don't do that
                guiGraphics.blit(COMPONENT, position.x - 1, position.y - 1, 0, 204, 18, 18);
            }

        }
        pose.popPose();
    }
}
