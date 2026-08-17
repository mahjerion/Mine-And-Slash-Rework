package com.robertx22.mine_and_slash.capability.player.container;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.mixin_ducks.MouseHandlerDuck;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {


    public static final ResourceLocation BACKGROUND_LOCATION = new ResourceLocation(SlashRef.MODID, "textures/gui/master_bag.png");

    // mouse handler positions are in window coordinates, both of these have to come from the same space
    public static double iMouseX = defaultMouseX();
    public static double iMouseY = defaultMouseY();

    private static double defaultMouseX() {
        return Minecraft.getInstance().getWindow().getScreenWidth() / 2D;
    }

    private static double defaultMouseY() {
        return Minecraft.getInstance().getWindow().getScreenHeight() / 2D;
    }

    // the png grew to 256x512 to fit a 6th tab cell. the short GuiGraphics.blit overload hardcodes
    // a 256x256 sheet, so every uv has to be passed with these explicitly or the whole gui samples
    // from half the intended height
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 512;

    private static final int TEXTURE_TOP_Y = 0;
    private static final int TEXTURE_TOP_HEIGHT = 17;
    private static final int TEXTURE_ROW_Y = TEXTURE_TOP_HEIGHT;
    private static final int TEXTURE_ROW_HEIGHT = 18;
    private static final int TEXTURE_BOTTOM_Y = TEXTURE_ROW_Y + TEXTURE_ROW_HEIGHT;
    private static final int TEXTURE_BOTTOM_HEIGHT = 99;
    private static final int TEXTURE_TABS_Y = TEXTURE_BOTTOM_Y + TEXTURE_BOTTOM_HEIGHT;
    // one 18px cell per tab plus an 8px border top and bottom
    private static final int TEXTURE_TABS_HEIGHT = Backpacks.BackpackType.values().length * 18 + 16;

    private static final int ROW_START_Y = 16;

    protected int rows;
    // rows in the container, which on the currency and socketable tabs is more than fits on screen
    protected int totalRows;
    protected int scrollRow = 0;

    // init also runs on resize, the cursor may only be moved when the screen first opens
    private boolean restoredMousePos = false;

    public BackpackScreen(BackpackMenu pMenu, Inventory pPlayerInventory, Component txt) {
        super(pMenu, pPlayerInventory, Component.literal(""));
        this.rows = pMenu.getVisibleRows();
        this.totalRows = pMenu.getTotalRows();
        this.imageWidth = 199;
        // - 1 because the top pixel of the bottom texture overlaps the bottom pixel of the bottom row
        this.imageHeight = ROW_START_Y + TEXTURE_ROW_HEIGHT * rows + TEXTURE_BOTTOM_HEIGHT - 1;
    }

    private int maxScrollRow() {
        return Math.max(0, totalRows - rows);
    }

    // the tab strip is centred against the slot area, which changes height per tab
    private int tabsOffsetY() {
        return (ROW_START_Y + TEXTURE_ROW_HEIGHT * rows - TEXTURE_TABS_HEIGHT) / 2;
    }

    /**
     * Slides the grid through the window.
     * <p>
     * Slot.x/y are mutable, and AbstractContainerScreen gates both rendering and hit testing on
     * isActive(), so switching the off-screen rows inactive is enough to take them out of play.
     */
    private void updateSlotPositions() {
        scrollRow = Mth.clamp(scrollRow, 0, maxScrollRow());

        for (Slot slot : this.menu.slots) {
            if (slot instanceof BackpackMenu.BackpackSlot bag) {
                int row = bag.getContainerSlot() / 9;
                int visibleRow = row - scrollRow;

                bag.visible = visibleRow >= 0 && visibleRow < rows;
                if (bag.visible) {
                    bag.y = 18 + visibleRow * 18;
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScrollRow() > 0) {
            scrollRow -= (int) Math.signum(delta);
            updateSlotPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        super.init();

        updateSlotPositions();

        int x = leftPos + 175;
        int y = topPos + tabsOffsetY() + 9;

        for (Backpacks.BackpackType type : Backpacks.BackpackType.values()) {
            this.addRenderableWidget(new BackpackButton(type, x, y));
            y += 18;
        }

        if (!restoredMousePos) {
            restoredMousePos = true;

            MouseHandlerDuck mouseHandler = (MouseHandlerDuck) Minecraft.getInstance().mouseHandler;
            //init() will be invoked when this screen be set to Minecraft.screen after the releaseMouse(), see setScreen();
            mouseHandler.setXPos(iMouseX);
            mouseHandler.setYPos(iMouseY);
            //from MouseHandler.class releaseMouse()
            InputConstants.grabOrReleaseMouse(this.minecraft.getWindow().getWindow(), 212993, iMouseX, iMouseY);
        }
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
        iMouseX = defaultMouseX();
        iMouseY = defaultMouseY();
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
            blitBg(pGuiGraphics, x, rowY, TEXTURE_ROW_Y, TEXTURE_ROW_HEIGHT);
            rowY += TEXTURE_ROW_HEIGHT;
        }

        // Draw top and bottom, covering the top/bottom 1px of the repeating textures
        blitBg(pGuiGraphics, x, y, TEXTURE_TOP_Y, TEXTURE_TOP_HEIGHT);
        blitBg(pGuiGraphics, x, rowY - 1, TEXTURE_BOTTOM_Y, TEXTURE_BOTTOM_HEIGHT);

        // Draw tabs in the middle of the upper section
        blitBg(pGuiGraphics, x, y + tabsOffsetY(), TEXTURE_TABS_Y, TEXTURE_TABS_HEIGHT);

        drawScrollHints(pGuiGraphics, x, y);
    }

    // full-width strip of the background sheet, with the real texture size so the uvs land right
    private void blitBg(GuiGraphics gui, int x, int y, int v, int height) {
        gui.blit(BACKGROUND_LOCATION, x, y, 0f, (float) v, this.imageWidth, height, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    // no scrollbar art, so a pair of small chevrons in the right margin is the cue that the grid
    // continues past the window
    private void drawScrollHints(GuiGraphics gui, int x, int y) {
        if (maxScrollRow() <= 0) {
            return;
        }
        int arrowX = x + 171;
        if (scrollRow > 0) {
            drawChevron(gui, arrowX, y + ROW_START_Y + 2, true);
        }
        if (scrollRow < maxScrollRow()) {
            drawChevron(gui, arrowX, y + ROW_START_Y + TEXTURE_ROW_HEIGHT * rows - 4, false);
        }
    }

    private void drawChevron(GuiGraphics gui, int x, int y, boolean up) {
        int color = 0xFFBFB5A0;
        for (int i = 0; i < 3; i++) {
            int row = up ? y + i : y + 2 - i;
            gui.fill(x + (2 - i), row, x + (3 + i), row + 1, color);
        }
    }

    // copy of AbstractContainerScreen#renderSlot, the only change is that the stack count is drawn by
    // drawStackSize so big counts can be scaled down. keep the quick craft branch in sync with vanilla
    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        int x = slot.x;
        int y = slot.y;
        ItemStack stack = slot.getItem();
        boolean highlight = false;
        boolean skipItem = slot == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
        ItemStack carried = this.menu.getCarried();
        String countOverride = null;

        if (slot == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !stack.isEmpty()) {
            stack = stack.copyWithCount(stack.getCount() / 2);
        } else if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !carried.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                return;
            }

            if (AbstractContainerMenu.canItemQuickReplace(slot, carried, true) && this.menu.canDragTo(slot)) {
                highlight = true;
                int max = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                int inSlot = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                int placed = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, this.quickCraftingType, carried) + inSlot;
                if (placed > max) {
                    placed = max;
                    countOverride = ChatFormatting.YELLOW.toString() + max;
                }

                stack = carried.copyWithCount(placed);
            } else {
                this.quickCraftSlots.remove(slot);
                this.recalculateQuickCraftRemaining();
            }
        }

		PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 100.0F);

        if (stack.isEmpty() && slot.isActive()) {
            Pair icon = slot.getNoItemIcon();
            if (icon != null) {
                TextureAtlasSprite sprite = (TextureAtlasSprite)this.minecraft.getTextureAtlas((ResourceLocation)icon.getFirst()).apply((ResourceLocation)icon.getSecond());
                guiGraphics.blit(x, y, 0, 16, 16, sprite);
                skipItem = true;
            } else {
                ItemStack ghost = getGhostStack(slot);
                if (!ghost.isEmpty()) {
                    renderGhostItem(guiGraphics, ghost, x, y);
                    skipItem = true;
                }
            }
        }

        if (!skipItem && !stack.isEmpty()) {
            if (highlight) {
                guiGraphics.fill(x, y, x + 16, y + 16, this.getSlotColor(slot.index));
            }
            guiGraphics.renderItem(stack, x, y, slot.x + slot.y * this.imageWidth);
            guiGraphics.renderItemDecorations(this.font, stack, x, y, countOverride == null ? "" : countOverride);
            if (countOverride == null && stack.getCount() != 1) {
                drawStackSize(guiGraphics, formatStackSize(stack), x, y);
            }
        }

        pose.popPose();
    }

    // the item a reserved slot is waiting for, so the empty slot can advertise what belongs there
    private ItemStack getGhostStack(Slot slot) {
        return slot instanceof BackpackMenu.BackpackSlot bag ? bag.ghostStack : ItemStack.EMPTY;
    }

    /**
     * Draws the item faded, to say "this is where this goes, you just don't have one".
     * <p>
     * Done by painting the slot colour back over the item rather than by lowering the shader
     * alpha: RenderType.entityCutout has no transparency state, so a lot of items would come out
     * fully opaque and the tab would look like you owned half of it.
     */
    private void renderGhostItem(GuiGraphics guiGraphics, ItemStack ghost, int x, int y) {
        guiGraphics.renderItem(ghost, x, y);
        // items are batched, so they have to be on screen before we paint over them
        guiGraphics.flush();

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        // above the 150 the item model occupies, same trick renderItemDecorations uses
        pose.translate(0f, 0f, 200f);
        guiGraphics.fill(x, y, x + 16, y + 16, GHOST_DIM_COLOR);
        pose.popPose();
        guiGraphics.flush();
    }

    // the slot interior colour from master_bag.png, at the opacity that leaves the icon just readable
    private static final int GHOST_DIM_COLOR = 0xBB3E3835;

    // vanilla only tooltips slots that hold something, but naming the reserved item is the whole
    // point of the ghost icon
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            ItemStack ghost = getGhostStack(this.hoveredSlot);
            if (!ghost.isEmpty()) {
                guiGraphics.renderTooltip(this.font, this.getTooltipFromContainerItem(ghost), ghost.getTooltipImage(), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // From SophisticatedCore
    protected void drawStackSize(GuiGraphics guiGraphics, String count, int x, int y) {
		PoseStack pose = guiGraphics.pose();
		pose.pushPose();
		pose.translate(0f, 0f, 200f);
		float scale = Math.min(1f, 16f / font.width(count));
		if (scale < 1f) {
			pose.scale(scale, scale, 1f);
		}
		guiGraphics.drawString(font, count, (x + 19 - 2 - (font.width(count) * scale)) / scale, (y + 6 + 3 + (1 / (scale * scale) - 1)) / scale, 0xFFFFFF, true);
		pose.popPose();
    }

    protected String formatStackSize(ItemStack stack) {
        int count = stack.getCount();
        if (count >= 10000) {
            return String.format("%d.%dk", count / 1000, count / 100 % 10);
        } else {
            return String.valueOf(count);
        }
    }
}
