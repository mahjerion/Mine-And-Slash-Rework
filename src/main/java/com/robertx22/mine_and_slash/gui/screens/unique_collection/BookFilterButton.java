package com.robertx22.mine_and_slash.gui.screens.unique_collection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A dropdown: shows the current selection, opens the option list on click. Same two part shape the wiki
 * uses, kept local because the wiki's version is typed against NewWikiScreen.
 */
public class BookFilterButton extends AbstractButton {

    public static final int WIDTH = 100;
    public static final int HEIGHT = 20;

    private final UniqueCollectionScreen screen;
    private final BookFilterCategory category;

    public BookFilterButton(UniqueCollectionScreen screen, BookFilterCategory category, int x, int y) {
        super(x, y, WIDTH, HEIGHT, category.label.locName());
        this.screen = screen;
        this.category = category;
    }

    @Override
    public void onPress() {
        Minecraft.getInstance().setScreen(new BookFilterSelectScreen(screen, category));
    }

    @Override
    public Component getMessage() {
        BookFilter cur = screen.getFilter(category);
        // unset shows the category, not "Pick Slot" - the wiki's wording only reads right for its one
        // slot filter, and these buttons are drop level and map tier as well
        if (cur == BookFilter.NONE) {
            return category.label.locName();
        }
        return cur.name;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
    }
}
