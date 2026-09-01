package com.robertx22.mine_and_slash.gui.screens.unique_collection;

import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import net.minecraft.world.item.ItemStack;

/**
 * One page of the sticker book: a unique, plus the preview stack the grid draws for it.
 * <p>
 * The stack is a real rolled item, built once and reused - re-rolling it every frame would be
 * ruinous with a couple of hundred entries on screen.
 */
public class UniqueBookEntry {

    public final UniqueGear unique;
    public final ItemStack preview;
    public final String searchName;

    public UniqueBookEntry(UniqueGear unique, ItemStack preview, String searchName) {
        this.unique = unique;
        this.preview = preview;
        this.searchName = searchName;
    }
}
