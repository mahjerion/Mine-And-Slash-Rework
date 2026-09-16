package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.mine_and_slash.gui.inv_gui.GuiInventoryGrids;
import com.robertx22.mine_and_slash.gui.inv_gui.InvGuiGrid;
import com.robertx22.mine_and_slash.gui.inv_gui.InvGuiScreen;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenarySlotType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

/**
 * The "pick something for this mercenary slot" grid. Paged, because the player's inventory and a full
 * master bag tab together can hold far more legal entries than the 54 slot grid, and
 * {@code InvGuiGrid.ofList} drops the overflow without a word.
 * <p>
 * Same shape as {@code SalvageSubFilterScreen}, which pages the same grid for the same reason.
 */
public class MercEquipPickerScreen extends InvGuiScreen {

    static int BUTTON_SIZE_X = 55;
    static int BUTTON_SIZE_Y = 16;

    final MercenarySlotType type;
    final int index;
    final int page;

    public MercEquipPickerScreen(MercenarySlotType type, int index, int page) {
        super(gridFor(type, index, page),
                Words.Mercenary.locName(), () -> ClientOnly.setScreen(new MercenaryScreen()));

        this.type = type;
        this.index = index;
        this.page = page;
    }

    // super() has to be the first statement, so the null player case cannot be handled inline
    private static InvGuiGrid gridFor(MercenarySlotType type, int index, int page) {
        Player p = ClientOnly.getPlayer();
        if (p == null) {
            return InvGuiGrid.ofList(new ArrayList<>());
        }
        return GuiInventoryGrids.ofMercSlotChoices(p, type, index, page);
    }

    /** a fresh master bag snapshot arrived, so the entries may have changed under us */
    public void refresh() {
        Player p = ClientOnly.getPlayer();
        if (p == null) {
            return;
        }
        this.grid = GuiInventoryGrids.ofMercSlotChoices(p, type, index, page);
        rebuildWidgets();
    }

    private int pageCount() {
        Player p = ClientOnly.getPlayer();
        return p == null ? 1 : GuiInventoryGrids.mercSlotChoicePageCount(p, type, index);
    }

    @Override
    protected void init() {
        super.init();

        int pages = pageCount();
        int y = guiTop + sizeY + 2;

        // the corner arrow is the way back, so Prev/Next centre as a pair
        if (page > 0) {
            publicAddButton(Button.builder(Words.SalvagePrevPage.locName(), b -> ClientOnly.setScreen(new MercEquipPickerScreen(type, index, page - 1)))
                    .bounds(guiLeft + sizeX / 2 - BUTTON_SIZE_X - 2, y, BUTTON_SIZE_X, BUTTON_SIZE_Y)
                    .build());
        }
        if (page < pages - 1) {
            publicAddButton(Button.builder(Words.SalvageNextPage.locName(), b -> ClientOnly.setScreen(new MercEquipPickerScreen(type, index, page + 1)))
                    .bounds(guiLeft + sizeX / 2 + 2, y, BUTTON_SIZE_X, BUTTON_SIZE_Y)
                    .build());
        }
    }
}
