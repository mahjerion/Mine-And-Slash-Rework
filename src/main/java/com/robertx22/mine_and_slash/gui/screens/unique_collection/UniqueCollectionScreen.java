package com.robertx22.mine_and_slash.gui.screens.unique_collection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import com.robertx22.mine_and_slash.database.data.unique_items.collection.ShardMath;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.vanilla_mc.packets.unique_collection.ClientUniqueCollection;
import com.robertx22.mine_and_slash.vanilla_mc.packets.unique_collection.CraftUniquePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The unique sticker book.
 * <p>
 * Deliberately NOT a container screen. Nothing is stored here - it is a record of what has been
 * collected - so there is no inventory, no MenuType and no slot syncing to get wrong. It reads the
 * client mirror of the collection and asks the server to reconstruct; the server re-derives and
 * re-validates everything, so nothing on this screen is trusted.
 * <p>
 * Built on the same 177x136 panel and 9x6 grid as the auto salvage config screen, and like every screen
 * in that family it never calls renderBackground, so the world stays visible behind it.
 */
public class UniqueCollectionScreen extends BaseScreen {

    private static final ResourceLocation TEX = SlashRef.guiId("inv_gui/background");

    private static final int PANEL_W = 177;
    private static final int PANEL_H = 136;

    // grid geometry copied from InvGuiScreen.init - the panel art draws the slots at these offsets
    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int CELL = 18;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 17;

    // filter button + gap + clear button + gap to the panel
    private static final int LEFT_COLUMN = BookFilterButton.WIDTH + 2 + ClearBookFilterButton.SIZE + 4;

    private static final int SEARCH_W = 140;
    private static final int SEARCH_H = 20;

    // room above the panel for the search box (guiTop - 24) and the title (guiTop - 36)
    private static final int TOP_MARGIN = 40;

    /**
     * Locked entries are painted over rather than alpha blended: RenderType.entityCutout carries no
     * transparency state, so lowering the shader alpha leaves many items fully opaque.
     * <p>
     * Neutral dark rather than a slot colour - this reads as the silhouette the feature is meant to show,
     * and doesn't depend on what the panel texture's slot interior happens to be.
     */
    private static final int LOCKED_SHADOW_COLOR = 0xB0000000;
    private static final int SLOT_HOVER_COLOR = 0x80FFFFFF;
    private static final int CHEVRON_COLOR = 0xFFBFB5A0;

    private final BlockPos station;

    private EditBox searchBox;
    private String search = "";

    private final Map<BookFilterCategory, BookFilter> filters = new EnumMap<>(BookFilterCategory.class);

    private List<UniqueBookEntry> visible = new ArrayList<>();
    private int scrollRow = 0;

    public UniqueCollectionScreen(BlockPos station) {
        super(PANEL_W, PANEL_H);
        this.station = station;
    }

    public static int playerLevel() {
        return Load.Unit(ClientOnly.getPlayer()).getLevel();
    }

    public BookFilter getFilter(BookFilterCategory category) {
        return filters.getOrDefault(category, BookFilter.NONE);
    }

    public void setFilter(BookFilterCategory category, BookFilter filter) {
        filters.put(category, filter);
        // the scroll position is meaningless against a different result set
        scrollRow = 0;
        rebuildVisible();
    }

    @Override
    protected void init() {
        super.init();

        // the panel itself is centred, so it sits where every other station screen does - the filter
        // column just hangs off its left. the floor only engages below ~429 scaled width, where that
        // column would otherwise run off the edge.
        this.guiLeft = Math.max(LEFT_COLUMN, this.guiLeft);
        // the title and search box hang above the panel, so it can't sit flush against the top edge
        this.guiTop = Math.max(TOP_MARGIN, this.guiTop);

        this.searchBox = new EditBox(this.font, guiLeft + sizeX / 2 - SEARCH_W / 2, guiTop - 24,
                SEARCH_W, SEARCH_H, this.searchBox, Component.literal(""));
        this.searchBox.setValue(this.search);
        this.searchBox.setResponder(s -> {
            this.search = s;
            this.scrollRow = 0;
            rebuildVisible();
        });
        this.addWidget(this.searchBox);
        // so the player can just start typing a unique's name on open
        this.setInitialFocus(this.searchBox);

        // flush with the panel's top edge, running down its left side
        int y = guiTop;
        for (BookFilterCategory category : BookFilterCategory.values()) {
            this.addRenderableWidget(new BookFilterButton(this, category, guiLeft - LEFT_COLUMN, y));
            this.addRenderableWidget(new ClearBookFilterButton(this, category,
                    guiLeft - ClearBookFilterButton.SIZE - 4, y));
            y += BookFilterButton.HEIGHT + 4;
        }

        rebuildVisible();
    }

    private void rebuildVisible() {
        String query = this.search.toLowerCase(Locale.ROOT).trim();
        List<UniqueBookEntry> list = new ArrayList<>();

        for (UniqueBookEntry entry : UniqueBookCache.get(playerLevel())) {
            if (!query.isEmpty() && !entry.searchName.contains(query)) {
                continue;
            }
            boolean passes = true;
            for (BookFilter f : filters.values()) {
                if (!f.isValid(entry.unique)) {
                    passes = false;
                    break;
                }
            }
            if (passes) {
                list.add(entry);
            }
        }
        this.visible = list;
        this.scrollRow = Mth.clamp(this.scrollRow, 0, maxScrollRow());
    }

    private int maxScrollRow() {
        int totalRows = (this.visible.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, totalRows - ROWS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScrollRow() > 0) {
            this.scrollRow = Mth.clamp(this.scrollRow - (int) Math.signum(delta), 0, maxScrollRow());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = indexAt(mouseX, mouseY);
        if (index > -1) {
            tryCraft(this.visible.get(index));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void tryCraft(UniqueBookEntry entry) {
        // every one of these is re-checked on the server. this is only here so the click gives
        // immediate feedback instead of a round trip that ends in a chat error.
        if (!ClientUniqueCollection.isUnlocked(entry.unique.GUID())
                || playerLevel() < entry.unique.min_drop_lvl
                || !ClientUniqueCollection.canAfford(ShardMath.craftCost(entry.unique, playerLevel()))) {
            ClientOnly.getPlayer().playSound(SoundEvents.VILLAGER_NO, 0.5F, 1F);
            return;
        }
        Packets.sendToServer(new CraftUniquePacket(entry.unique.GUID(), this.station));
        ClientOnly.getPlayer().playSound(SoundEvents.PLAYER_LEVELUP, 0.4F, 1.5F);
    }

    private int cellX(int col) {
        return guiLeft + GRID_X + col * CELL;
    }

    private int cellY(int row) {
        return guiTop + GRID_Y + row * CELL;
    }

    private int indexAt(double mouseX, double mouseY) {
        int col = (int) Math.floor((mouseX - (guiLeft + GRID_X)) / CELL);
        int row = (int) Math.floor((mouseY - (guiTop + GRID_Y)) / CELL);
        if (col < 0 || col >= COLUMNS || row < 0 || row >= ROWS) {
            return -1;
        }
        int index = (row + this.scrollRow) * COLUMNS + col;
        return index < this.visible.size() ? index : -1;
    }

    @Override
    public void tick() {
        this.searchBox.tick();
    }


    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        // deliberately no renderBackground: the world stays visible behind the panel, like every other
        // screen in the inv_gui family
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        gui.blit(TEX, guiLeft, guiTop, 0, 0, sizeX, sizeY);

        super.render(gui, mouseX, mouseY, delta);

        renderTitleAndSearch(gui, mouseX, mouseY, delta);
        renderGrid(gui, mouseX, mouseY);
        renderFooter(gui);

        // last, so the tooltip sits above the panel and the filter buttons
        renderHoveredTooltip(gui, mouseX, mouseY);
    }

    private void renderTitleAndSearch(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        Component title = Words.UNIQUE_COLLECTION.locName();
        gui.drawString(this.font, title,
                guiLeft + sizeX / 2 - this.font.width(title) / 2, guiTop - 36,
                ChatFormatting.YELLOW.getColor(), true);

        this.searchBox.render(gui, mouseX, mouseY, delta);
    }

    private void renderFooter(GuiGraphics gui) {
        List<UniqueBookEntry> all = UniqueBookCache.get(playerLevel());
        int collected = (int) all.stream()
                .filter(e -> ClientUniqueCollection.isUnlocked(e.unique.GUID())).count();

        Component shards = Words.PHILOSOPHERS_SHARDS.locName()
                .append(": " + ClientUniqueCollection.shards)
                .withStyle(ChatFormatting.AQUA);
        Component progress = Words.COLLECTION_PROGRESS.locName(collected, all.size())
                .withStyle(ChatFormatting.GRAY);

        drawCenteredOnPanel(gui, shards, guiTop + sizeY + 4);
        drawCenteredOnPanel(gui, progress, guiTop + sizeY + 15);
    }

    private void drawCenteredOnPanel(GuiGraphics gui, Component text, int y) {
        gui.drawString(this.font, text, guiLeft + sizeX / 2 - this.font.width(text) / 2, y, 0xFFFFFF, true);
    }

    private void renderGrid(GuiGraphics gui, int mouseX, int mouseY) {
        int hovered = indexAt(mouseX, mouseY);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (row + this.scrollRow) * COLUMNS + col;
                if (index >= this.visible.size()) {
                    continue; // the panel art already draws an empty slot here
                }
                UniqueBookEntry entry = this.visible.get(index);
                int x = cellX(col);
                int y = cellY(row);

                if (ClientUniqueCollection.isUnlocked(entry.unique.GUID())) {
                    gui.renderItem(entry.preview, x + 1, y + 1);
                } else {
                    renderLockedItem(gui, entry, x + 1, y + 1);
                }

                if (index == hovered) {
                    gui.fill(x + 1, y + 1, x + 17, y + 17, SLOT_HOVER_COLOR);
                }
            }
        }

        drawScrollHints(gui);
    }

    /**
     * See {@link #LOCKED_SHADOW_COLOR} for why this paints over instead of fading.
     */
    private void renderLockedItem(GuiGraphics gui, UniqueBookEntry entry, int x, int y) {
        gui.renderItem(entry.preview, x, y);
        // items are batched, so they have to be on screen before anything paints over them
        gui.flush();

        PoseStack pose = gui.pose();
        pose.pushPose();
        // above the 150 the item model occupies, the same offset renderItemDecorations uses
        pose.translate(0f, 0f, 200f);
        gui.fill(x, y, x + 16, y + 16, LOCKED_SHADOW_COLOR);
        pose.popPose();
        gui.flush();
    }

    // the panel art has no scrollbar channel, so a pair of small chevrons in its right margin is the cue
    // that the grid continues. same treatment the backpack's currency tab uses.
    private void drawScrollHints(GuiGraphics gui) {
        if (maxScrollRow() <= 0) {
            return;
        }
        int arrowX = guiLeft + 171;
        if (this.scrollRow > 0) {
            drawChevron(gui, arrowX, cellY(0) + 2, true);
        }
        if (this.scrollRow < maxScrollRow()) {
            drawChevron(gui, arrowX, cellY(ROWS) - 4, false);
        }
    }

    private void drawChevron(GuiGraphics gui, int x, int y, boolean up) {
        for (int i = 0; i < 3; i++) {
            int row = up ? y + i : y + 2 - i;
            gui.fill(x + (2 - i), row, x + (3 + i), row + 1, CHEVRON_COLOR);
        }
    }

    private void renderHoveredTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        int index = indexAt(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        UniqueBookEntry entry = this.visible.get(index);
        UniqueGear unique = entry.unique;

        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, entry.preview));

        boolean unlocked = ClientUniqueCollection.isUnlocked(unique.GUID());
        int cost = ShardMath.craftCost(unique, playerLevel());

        lines.add(Component.empty());
        if (!unlocked) {
            lines.add(Words.UNIQUE_NOT_COLLECTED.locName().withStyle(ChatFormatting.DARK_GRAY));
        } else if (playerLevel() < unique.min_drop_lvl) {
            lines.add(Words.SHARD_COST.locName(cost).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Words.MIN_DROP_LEVEL.locName().append(String.valueOf(unique.min_drop_lvl))
                    .withStyle(ChatFormatting.RED));
        } else {
            lines.add(Words.SHARD_COST.locName(cost).withStyle(
                    ClientUniqueCollection.canAfford(cost) ? ChatFormatting.AQUA : ChatFormatting.RED));
            if (ClientUniqueCollection.canAfford(cost)) {
                // the crafted item comes out at the player's level, so name it - the preview stack above
                // is rolled at that same level and the number is what makes that obvious
                lines.add(Words.RECONSTRUCT.locName(playerLevel()).withStyle(ChatFormatting.GREEN));
            }
        }

        gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }
}
