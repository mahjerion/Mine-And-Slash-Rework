package com.robertx22.mine_and_slash.gui.screens.map;

import com.robertx22.dungeon_realm.client.DungeonStatsOverlay;
import com.robertx22.dungeon_realm.client.DungeonStatsStore;
import com.robertx22.dungeon_realm.item.DungeonItemMapData;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.dungeon_realm.main.DungeonEntries;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.library_of_exile.utils.GuiUtils;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.gui.bases.IAlertScreen;
import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.maps.MapData;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.vanilla_mc.packets.MapCompletePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

public class MapScreen extends BaseScreen implements INamedScreen, IAlertScreen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(SlashRef.MODID, "textures/gui/map/background.png");

    static int sizeX = 250;
    static int sizeY = 233;

    // matches DungeonStatsOverlay's own nine-patch padding (contentPadding 2 + CORNER_SIZE 8),
    // needed here to size/center the mimicked panel the same way the HUD does
    private static final int STATS_PANEL_PADDING = 10;

    // vertical gap between a panel/button and whatever is stacked below it
    private static final int PANEL_GAP = 8;

    // center of the stats panel (mimics DungeonStatsOverlay's HUD placement)
    private static final int STATS_PANEL_CENTER_X = 187;
    private static final int STATS_PANEL_CENTER_Y = 90;

    // map item icon, drawn manually so its hover tooltip can be scaled to fit the window.
    // offsets keep the old ItemButton placement (button at +56/+58, item drawn at button +1).
    private static final int ICON_X_OFFSET = 57;
    private static final int ICON_Y_OFFSET = 59;
    private static final int ICON_SIZE = 16;
    private static final int ICON_CENTER_X = ICON_X_OFFSET + ICON_SIZE / 2;
    private static final int ICON_BOTTOM_Y = ICON_Y_OFFSET + ICON_SIZE;

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
            publicAddButton(new MapRarityButton(guiLeft + sizeX / 2 - MapRarityButton.SIZE / 2, guiTop + 9));
            publicAddButton(new MapBarButton(guiLeft + 11, guiTop + 207));

            if (DungeonStatsStore.isBossTeleportUnlocked()) {
                int buttonX = guiLeft + STATS_PANEL_CENTER_X - TeleportToBossButton.WIDTH / 2;
                int buttonY = ticketsPanelBottomY() + PANEL_GAP;
                publicAddButton(new TeleportToBossButton(buttonX, buttonY));
            }

            addBackToHubButton();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // prefer the real snapshot of the map item taken when it was used, so its full tooltip
    // (uber/pinnacle, applied mods) shows on hover; fall back to a minimal reconstruction for
    // maps started before snapshots existed. resolved each frame so a snapshot arriving while the
    // screen is open is picked up.
    private ItemStack resolveMapStack() {
        ItemStack stack = DungeonStatsStore.getMapItem();
        if (stack != null && !stack.isEmpty()) {
            return stack;
        }
        DungeonItemMapData data = new DungeonItemMapData();
        data.dungeon = DungeonStatsStore.getMapDungeon();
        data.uber = DungeonStatsStore.isMapUber();

        stack = DungeonEntries.DUNGEON_MAP_ITEM.get().getDefaultInstance();
        DungeonItemNbt.DUNGEON_MAP.saveTo(stack, data);
        return stack;
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, float ticks) {
        try {
            gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            gui.blit(BACKGROUND, mc.getWindow().getGuiScaledWidth() / 2 - sizeX / 2,
                    mc.getWindow().getGuiScaledHeight() / 2 - sizeY / 2, 0, 0, sizeX, sizeY);

            renderStatsPanel(gui);
            renderEntryTicketsPanel(gui);
            renderLeagueContentPanel(gui);

            ItemStack mapStack = resolveMapStack();
            int iconX = guiLeft + ICON_X_OFFSET;
            int iconY = guiTop + ICON_Y_OFFSET;
            gui.renderItem(mapStack, iconX, iconY);
            gui.renderItemDecorations(mc.font, mapStack, iconX, iconY);

            super.render(gui, x, y, ticks);

            // rendered last (on top) and scaled to fit, so a long map tooltip stays inside the window
            if (GuiUtils.isInRect(iconX, iconY, ICON_SIZE, ICON_SIZE, x, y)) {
                renderFittedTooltip(gui, mapStack, x, y);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Vanilla only edge-clamps tooltips, so one taller/wider than the window clips off-screen on small
    // windows or large GUI scales. Shrink by a uniform scale until the box fits, then let vanilla's
    // clamp position it: feeding mouse/scale inside a scale(s) pose keeps the final box within the window.
    private void renderFittedTooltip(GuiGraphics gui, ItemStack stack, int mouseX, int mouseY) {
        List<Component> lines = stack.getTooltipLines(mc.player, TooltipFlag.NORMAL);
        if (lines.isEmpty()) {
            return;
        }

        int maxWidth = 0;
        for (Component line : lines) {
            maxWidth = Math.max(maxWidth, mc.font.width(line));
        }
        int boxW = maxWidth + 8;
        int boxH = lines.size() * 10 + 8;

        float scale = Math.min(1f, Math.min((gui.guiWidth() - 8f) / boxW, (gui.guiHeight() - 8f) / boxH));

        gui.pose().pushPose();
        gui.pose().scale(scale, scale, 1f);
        gui.renderComponentTooltip(mc.font, lines, (int) (mouseX / scale), (int) (mouseY / scale));
        gui.pose().popPose();
    }

    // fixed line count (not text-dependent), so this can be computed once in init() to place
    // the teleport-to-boss button, as well as every frame when actually rendering the panel.
    private int statsPanelBoxHeight() {
        return (mc.font.lineHeight * 4) + STATS_PANEL_PADDING * 2;
    }

    // bottom Y of the stats panel, used to stack the teleport-to-boss button directly underneath.
    private int statsPanelBottomY() {
        int centerY = guiTop + STATS_PANEL_CENTER_Y + 10;
        return centerY + statsPanelBoxHeight() / 2;
    }

    // The live Entry Ticket pool for the run the player is standing in. Sourced from
    // MapCompletePacket.SYNCED_DATA - OnServerTick already ships the whole MapData to the client
    // every 10 seconds while inside a map, and MapEntryTickets pushes an extra one the moment a
    // ticket is spent, so this needs no packet of its own.
    //
    // Null when there is nothing real to show: the static starts life as a blank MapData, and a
    // blank one never has a playerUuid, whereas MapData.newMap always sets one.
    private MapData syncedMap() {
        try {
            var synced = MapCompletePacket.SYNCED_DATA;
            if (synced == null || synced.data == null || synced.data.playerUuid.isEmpty()) {
                return null;
            }
            if (synced.data.maxEntryTickets() <= 0) {
                return null; // this rarity opted out of the mechanic entirely
            }
            return synced.data;
        } catch (Exception e) {
            return null;
        }
    }

    private int ticketsPanelBoxHeight() {
        return mc.font.lineHeight + STATS_PANEL_PADDING * 2;
    }

    // bottom Y of the tickets panel, or of the stats panel when there is no ticket data to show -
    // so the teleport-to-boss button stacks correctly either way.
    private int ticketsPanelBottomY() {
        if (syncedMap() == null) {
            return statsPanelBottomY();
        }
        return statsPanelBottomY() + PANEL_GAP + ticketsPanelBoxHeight();
    }

    // "Entry Tickets  n / m", directly under the kill%/loot% panel. Drawn as its own nine-patch box
    // rather than as a fourth line of the stats panel, because DungeonStatsOverlay.renderAt takes
    // exactly three components and lives in dungeon_realm.
    private void renderEntryTicketsPanel(GuiGraphics gui) {
        MapData map = syncedMap();
        if (map == null) {
            return;
        }

        var font = mc.font;

        int left = map.entryTicketsLeft();
        int max = map.maxEntryTickets();

        // amber at one ticket left, red once the map is sealed - the point where leaving is final
        ChatFormatting color = left <= 0 ? ChatFormatting.RED : (left == 1 ? ChatFormatting.GOLD : ChatFormatting.AQUA);

        Component line = Words.MAP_ENTRY_TICKETS.locName()
                .append(Component.literal(": " + left + " / " + max))
                .withStyle(color);

        int boxW = font.width(line) + STATS_PANEL_PADDING * 2;
        int boxH = ticketsPanelBoxHeight();

        int centerX = guiLeft + STATS_PANEL_CENTER_X;
        int x = centerX - boxW / 2;
        int y = statsPanelBottomY() + PANEL_GAP;

        DungeonStatsOverlay.renderNinePatchWithFallback(gui, x, y, boxW, boxH);
        gui.drawCenteredString(font, line, centerX, y + STATS_PANEL_PADDING, 0xFFFFFFFF);
    }

    // mimics the standard DungeonStatsOverlay HUD (rarity name + kill%/loot%), placed in the
    // right half of the gui, roughly halfway down.
    private void renderStatsPanel(GuiGraphics gui) {
        var font = mc.font;

        var mapRarityName = DungeonStatsOverlay.getMapRarityName();
        var killCompletion = DungeonStatsOverlay.getMapKillCompletion();
        var lootCompletion = DungeonStatsOverlay.getMapLootCompletion();

        int maxWidth = Math.max(Math.max(font.width(mapRarityName), font.width(killCompletion)), font.width(lootCompletion));

        int boxW = maxWidth + STATS_PANEL_PADDING * 2;
        int boxH = statsPanelBoxHeight();

        int centerX = guiLeft + STATS_PANEL_CENTER_X;
        int centerY = guiTop + STATS_PANEL_CENTER_Y + 10;

        int x = centerX - boxW / 2;
        int y = centerY - boxH / 2;

        DungeonStatsOverlay.renderAt(gui, x, y, boxW, boxH, mapRarityName, killCompletion, lootCompletion);
    }

    // below the cached map icon: names of league content (bonus encounters) rolled for this
    // map instance. Sourced from DungeonStatsStore.getBonusContentIds(), populated once at map
    // start via the same server->client stats packet as the stats panel. Hidden entirely when
    // nothing was rolled for this map, rather than showing an empty/placeholder box.
    private void renderLeagueContentPanel(GuiGraphics gui) {
        List<String> ids = DungeonStatsStore.getBonusContentIds();
        if (ids.isEmpty()) {
            return;
        }

        var font = mc.font;

        List<Component> lines = new ArrayList<>();
        lines.add(DungeonWords.DUNGEON_STATS_LEAGUE_CONTENT_HEADER.get().withStyle(ChatFormatting.YELLOW));
        for (String id : ids) {
            lines.add(DungeonWords.LeagueContentName(id));
        }

        int maxWidth = 0;
        for (Component line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }

        int boxW = maxWidth + STATS_PANEL_PADDING * 2;
        int boxH = font.lineHeight * lines.size() + STATS_PANEL_PADDING * 2;

        int centerX = guiLeft + ICON_CENTER_X;
        int x = centerX - boxW / 2;
        int topY = guiTop + ICON_BOTTOM_Y + PANEL_GAP + 4;

        DungeonStatsOverlay.renderNinePatchWithFallback(gui, x, topY, boxW, boxH);

        int ty = topY + STATS_PANEL_PADDING;
        for (Component line : lines) {
            gui.drawCenteredString(font, line, centerX, ty, 0xFFFFFFFF);
            ty += font.lineHeight;
        }
    }

    @Override
    public boolean shouldAlert() {
        return DungeonStatsStore.isBossTeleportUnlocked();
    }
}
