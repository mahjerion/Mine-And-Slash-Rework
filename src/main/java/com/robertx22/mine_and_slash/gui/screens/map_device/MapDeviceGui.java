package com.robertx22.mine_and_slash.gui.screens.map_device;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.resources.ResourceLocation;

/**
 * Every size and texture the map device screen draws with. The screen is a vertical stack - title,
 * optional map slot, 2x2 relic grid, action button, optional tickets line - so the y positions are
 * computed at layout time from these, not fixed; only the widths are. The Atlas button and the exit
 * arrow hang above the panel, outside it.
 */
public class MapDeviceGui {

    // ---------------------------------------------------------------- textures, all existing art

    /** one slot cell: 18px pitch */
    public static final int SLOT_SIZE = 18;
    /**
     * both slot textures have a 1px light frame and then a 1px dark shadow line before the 16x16 icon
     * area, so a cell's clickable square starts one pixel in from the cell origin and the icon one more
     */
    public static final int SLOT_INSET = 1;

    /** the map slot art, a single 19x19 cell (18 plus the shared closing border) */
    public static final ResourceLocation MAP_SLOT_TEX = SlashRef.guiId("atlas_map/map_slot");
    public static final int MAP_SLOT_TEX_SIZE = 19;

    /** the relic grid art, 2x2 cells on an 18px pitch plus the closing border: 37x37 */
    public static final ResourceLocation RELIC_SLOTS_TEX = SlashRef.guiId("atlas_map/relic_slots");
    public static final int RELIC_SLOTS_TEX_SIZE = 37;

    /** 79x21 two-frame sheet (idle on top, hover below), the same one the Atlas nav button uses */
    public static final ResourceLocation BUTTON = SlashRef.guiId("atlas_map/button");
    public static final int BUTTON_W = 79;
    public static final int BUTTON_H = 21;
    public static final int BUTTON_TEX_SIZE = 256;

    /**
     * vanilla's creative inventory scroller handle: 12x15 at u232 (active) / u244 (disabled) on the
     * 256x256 tabs sheet - the same handle every vanilla scroll list drags
     */
    public static final ResourceLocation SCROLLER_SHEET = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    public static final int SCROLLER_U = 232;
    public static final int SCROLLER_U_DISABLED = 244;
    public static final int SCROLLER_V = 0;
    public static final int SCROLLER_W = 12;
    public static final int SCROLLER_H = 15;
    public static final int SCROLLER_TEX_SIZE = 256;
    /** extra panel width reserved for the scrollbar column, only when the list overflows */
    public static final int SCROLLBAR_W = SCROLLER_W + 2;

    // the panels themselves are DungeonStatsOverlay's nine-patch, the same frame the map screen and
    // the in-map stats overlay use

    // ---------------------------------------------------------------- main panel

    /** minimum; the panel grows to fit the Entry Tickets line when that is wider */
    public static final int PANEL_W = 150;
    public static final int PAD_TOP = 14;
    public static final int PAD_BOTTOM = 12;
    /** title text plus the gap under it */
    public static final int TITLE_H = 14;
    public static final int ROW_GAP = 6;
    public static final int TICKETS_GAP = 5;
    public static final int TICKETS_H = 10;

    // ---------------------------------------------------------------- above the panel

    /** gap between the panel's top edge and the buttons hanging above it */
    public static final int TOP_BUTTON_GAP = 2;

    // ---------------------------------------------------------------- side panels

    /** space between the main panel and a side panel */
    public static final int SIDE_GAP = 4;
    /** nine-patch border (8) plus a little breathing room, matching MapScreen.STATS_PANEL_PADDING */
    public static final int SIDE_PAD = 10;
    public static final int LINE_H = 10;
    public static final int SIDE_MIN_W = 80;
}
