package com.robertx22.mine_and_slash.gui.screens.map;

import com.robertx22.dungeon_realm.client.DungeonStatsStore;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.resources.ResourceLocation;

public class MapRarityButton extends ImageButton {

    public static int SIZE = 34;

    public MapRarityButton(int xPos, int yPos) {
        super(xPos, yPos, SIZE, SIZE, 0, 0, 0, new ResourceLocation(SlashRef.MODID, ""), (button) -> {
        });
    }

    @Override
    public void onPress() {

    }

    @Override
    protected ClientTooltipPositioner createTooltipPositioner() {
        return DefaultTooltipPositioner.INSTANCE;
    }

    @Override
    public void renderWidget(GuiGraphics gui, int pMouseX, int pMouseY, float pPartialTick) {
        String rarity = DungeonStatsStore.getMapRarityId();
        if (rarity == null || rarity.isEmpty()) {
            rarity = "common";
        }
        ResourceLocation loc = new ResourceLocation(SlashRef.MODID, "textures/gui/map/rarity/" + rarity + ".png");
        gui.blit(loc, this.getX(), this.getY(), 0, 0, SIZE, SIZE, SIZE, SIZE);
    }

}
