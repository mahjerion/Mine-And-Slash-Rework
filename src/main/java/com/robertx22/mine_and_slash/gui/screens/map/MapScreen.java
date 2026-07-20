package com.robertx22.mine_and_slash.gui.screens.map;

import com.robertx22.dungeon_realm.client.DungeonStatsOverlay;
import com.robertx22.dungeon_realm.client.DungeonStatsStore;
import com.robertx22.dungeon_realm.item.DungeonItemMapData;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.dungeon_realm.main.DungeonEntries;
import com.robertx22.mine_and_slash.database.data.profession.screen.ItemButton;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.gui.bases.IAlertScreen;
import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class MapScreen extends BaseScreen implements INamedScreen, IAlertScreen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(SlashRef.MODID, "textures/gui/map/background.png");

    static int sizeX = 250;
    static int sizeY = 233;

    // matches DungeonStatsOverlay's own nine-patch padding (contentPadding 2 + CORNER_SIZE 8),
    // needed here to size/center the mimicked panel the same way the HUD does
    private static final int STATS_PANEL_PADDING = 10;

    Minecraft mc = Minecraft.getInstance();

    public MapScreen() {
        super(sizeX, sizeY);
    }

    @Override
    public ResourceLocation iconLocation() {
        return new ResourceLocation(SlashRef.MODID, "textures/gui/main_hub/icons/map.png");
    }

    @Override
    public Words screenName() {
        return Words.Map;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void init() {
        super.init();
        this.clearWidgets();

        try {
            DungeonItemMapData data = new DungeonItemMapData();
            data.dungeon = DungeonStatsStore.getMapDungeon();
            data.uber = DungeonStatsStore.isMapUber();

            ItemStack stack = DungeonEntries.DUNGEON_MAP_ITEM.get().getDefaultInstance();
            DungeonItemNbt.DUNGEON_MAP.saveTo(stack, data);

            publicAddButton(new MapRarityButton(guiLeft + sizeX / 2 - MapRarityButton.SIZE / 2, guiTop + 10));
            publicAddButton(new ItemButton(stack, guiLeft + 56, guiTop + 58));
            publicAddButton(new MapBarButton(guiLeft + 11, guiTop + 207));

            if (DungeonStatsStore.isBossTeleportUnlocked()) {
                publicAddButton(new TeleportToBossButton(guiLeft + 64 - TeleportToBossButton.WIDTH / 2, guiTop + 120));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, float ticks) {
        try {
            gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            gui.blit(BACKGROUND, mc.getWindow().getGuiScaledWidth() / 2 - sizeX / 2,
                    mc.getWindow().getGuiScaledHeight() / 2 - sizeY / 2, 0, 0, sizeX, sizeY);

            renderStatsPanel(gui);

            super.render(gui, x, y, ticks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // mimics the standard DungeonStatsOverlay HUD (rarity name + kill%/loot%), placed in the
    // right half of the gui, roughly halfway down
    private void renderStatsPanel(GuiGraphics gui) {
        var font = mc.font;

        var mapRarityName = DungeonStatsOverlay.getMapRarityName();
        var killCompletion = DungeonStatsOverlay.getMapKillCompletion();
        var lootCompletion = DungeonStatsOverlay.getMapLootCompletion();

        int maxWidth = Math.max(Math.max(font.width(mapRarityName), font.width(killCompletion)), font.width(lootCompletion));

        int boxW = maxWidth + STATS_PANEL_PADDING * 2;
        int boxH = (font.lineHeight * 4) + STATS_PANEL_PADDING * 2;

        int centerX = guiLeft + 187;
        int centerY = guiTop + 120;

        int x = centerX - boxW / 2;
        int y = centerY - boxH / 2;

        DungeonStatsOverlay.renderAt(gui, x, y, boxW, boxH, mapRarityName, killCompletion, lootCompletion);
    }

    @Override
    public boolean shouldAlert() {
        return DungeonStatsStore.isBossTeleportUnlocked();
    }
}
