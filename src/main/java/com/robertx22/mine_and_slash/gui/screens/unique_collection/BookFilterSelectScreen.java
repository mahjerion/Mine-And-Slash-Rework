package com.robertx22.mine_and_slash.gui.screens.unique_collection;

import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The dropdown's option list. Closing it always returns to the book, so a filter change never loses the
 * player's place.
 */
public class BookFilterSelectScreen extends Screen {

    private final UniqueCollectionScreen last;
    private final BookFilterCategory category;
    private OptionList list;

    public BookFilterSelectScreen(UniqueCollectionScreen last, BookFilterCategory category) {
        super(Gui.CHOOSE_FILTER.locName());
        this.last = last;
        this.category = category;
    }

    @Override
    protected void init() {
        this.list = new OptionList(this.minecraft, this.width, this.height, 48, this.height - 32, 24);
        this.addWidget(this.list);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.last);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        this.list.render(gui, mouseX, mouseY, delta);
        gui.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        super.render(gui, mouseX, mouseY, delta);
    }

    private void choose(BookFilter filter) {
        last.setFilter(category, filter);
        this.minecraft.setScreen(last);
    }

    private class OptionList extends ObjectSelectionList<OptionList.Row> {

        OptionList(Minecraft mc, int width, int height, int y0, int y1, int itemHeight) {
            super(mc, width, height, y0, y1, itemHeight);

            // "None" first so clearing is always the top entry, then the category's own options
            addEntry(new Row(BookFilter.NONE));
            for (BookFilter f : category.options.get()) {
                addEntry(new Row(f));
            }
        }

        class Row extends ObjectSelectionList.Entry<Row> {

            private final BookFilter filter;
            private final int count;

            Row(BookFilter filter) {
                this.filter = filter;
                this.count = (int) UniqueBookCache.get(UniqueCollectionScreen.playerLevel()).stream()
                        .filter(e -> filter.isValid(e.unique)).count();
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                choose(filter);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.empty();
            }

            @Override
            public void render(GuiGraphics gui, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float delta) {
                var text = filter.name.copy().append(" (" + count + ")");
                if (count < 1) {
                    // an option that would empty the grid is still selectable, just clearly marked
                    text.withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.GRAY);
                }
                gui.drawString(Minecraft.getInstance().font, text, left + 30, top + 6, ChatFormatting.YELLOW.getColor());
            }
        }
    }
}
