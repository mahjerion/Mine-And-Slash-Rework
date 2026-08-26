package com.robertx22.mine_and_slash.gui.inv_gui;

import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleAutoSalvageRarity;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

// the per gear type salvage filters of one salvage type. paged, because there can be more gear types than grid rows
public class SalvageSubFilterScreen extends InvGuiScreen {

    static int BUTTON_SIZE_X = 55;
    static int BUTTON_SIZE_Y = 16;

    ToggleAutoSalvageRarity.SalvageType type;
    int page;

    public SalvageSubFilterScreen(ToggleAutoSalvageRarity.SalvageType type, int page) {
        // this is a sub screen of the salvage config, so the corner arrow returns there rather than
        // to the hub
        super(GuiInventoryGrids.ofGearSubFilter(type, page), Words.Salvaging.locName(),
                () -> ClientOnly.setScreen(new SalvageConfigScreen()));

        this.type = type;
        this.page = page;
    }

    @Override
    protected void init() {
        super.init();

        int pages = GuiInventoryGrids.subFilterPageCount(type);
        int y = guiTop + sizeY + 2;

        // paging only - the corner arrow is the way back now, so Prev/Next centre as a pair rather
        // than flanking a Back button
        if (page > 0) {
            publicAddButton(Button.builder(Words.SalvagePrevPage.locName(), b -> ClientOnly.setScreen(new SalvageSubFilterScreen(type, page - 1)))
                    .bounds(guiLeft + sizeX / 2 - BUTTON_SIZE_X - 2, y, BUTTON_SIZE_X, BUTTON_SIZE_Y)
                    .build());
        }
        if (page < pages - 1) {
            publicAddButton(Button.builder(Words.SalvageNextPage.locName(), b -> ClientOnly.setScreen(new SalvageSubFilterScreen(type, page + 1)))
                    .bounds(guiLeft + sizeX / 2 + 2, y, BUTTON_SIZE_X, BUTTON_SIZE_Y)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, float ticks) {
        super.render(gui, x, y, ticks);

        int pages = GuiInventoryGrids.subFilterPageCount(type);

        Component title = pages > 1
                ? Component.empty().append(type.word.locName()).append(Component.literal(" (" + (page + 1) + "/" + pages + ")"))
                : type.word.locName();

        // the panel has no header space, so the title sits above it
        gui.drawString(mc.font, title, guiLeft + sizeX / 2 - mc.font.width(title) / 2, guiTop - 12, ChatFormatting.YELLOW.getColor(), true);
    }
}
