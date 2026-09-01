package com.robertx22.mine_and_slash.gui.screens.unique_collection;

import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.Predicate;

/**
 * One selectable option inside a filter category.
 * <p>
 * A plain predicate rather than the wiki's GroupFilterEntry, because those are typed against
 * BestiaryEntry and would mean wrapping every unique in one just to satisfy the signature. Everything
 * here runs client side, so predicates that need the player's own progress can simply read
 * ClientUniqueCollection.
 */
public class BookFilter {

    public static final BookFilter NONE = new BookFilter(Gui.NONE_FILTER.locName(), x -> true);

    public final MutableComponent name;
    public final Predicate<UniqueGear> test;

    public BookFilter(MutableComponent name, Predicate<UniqueGear> test) {
        this.name = name;
        this.test = test;
    }

    public boolean isValid(UniqueGear unique) {
        return test.test(unique);
    }
}
