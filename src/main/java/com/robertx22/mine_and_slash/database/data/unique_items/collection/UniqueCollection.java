package com.robertx22.mine_and_slash.database.data.unique_items.collection;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import com.robertx22.mine_and_slash.database.registry.ExileDB;

import java.util.Comparator;
import java.util.List;

/**
 * Which uniques the sticker book contains.
 * <p>
 * One predicate, used identically for display, unlock and craft validation. Keeping it in one place is
 * what stops the book from ever showing an entry the server would refuse to craft, or from recording an
 * unlock for an entry the book will never render.
 */
public class UniqueCollection {

    /**
     * League locked uniques (prophecy, uber, pinnacle, obelisk, harvest, strongbox, imprisoned_monster)
     * still salvage for shards but are deliberately never craftable.
     * <p>
     * Weight 0 is the retired sentinel. Craft to Exile 2 retires all 51 base mod uniques as weight 0
     * "_deprecated" stubs and re-authors them in per slot folders, so a legacy ItemStack can easily
     * still point at one - it must pay shards without creating a book entry that never renders.
     */
    public static boolean isInBook(UniqueGear unique) {
        if (unique == null) {
            return false;
        }
        return unique.weight > 0
                && !unique.isHiddenFromWiki()
                && unique.league.isEmpty();
    }

    /**
     * Every book entry, in a stable order.
     * <p>
     * Sorted by registry GUID, never by localized name - the client and the server both build this and
     * locname isn't serialized, so a name sort would give two different orders on a non English client.
     */
    private static List<UniqueGear> cached = null;

    static {
        ExileEvents.ON_REGISTER_TO_DATABASE.register(new EventConsumer<>() {
            @Override
            public void accept(ExileEvents.OnRegisterToDatabase e) {
                cached = null;
            }
        });
    }

    public static List<UniqueGear> all() {
        if (cached != null) {
            return cached;
        }
        List<UniqueGear> list = ExileDB.UniqueGears().getList().stream()
                .filter(UniqueCollection::isInBook)
                .sorted(Comparator.comparing(UniqueGear::GUID))
                .toList();

        // an empty result means the registry hasn't been populated yet - on a dedicated server the
        // client attaches capabilities before the registry sync lands. caching that would leave the
        // book permanently empty for the whole session.
        if (!list.isEmpty()) {
            cached = list;
        }
        return list;
    }

    public static UniqueGear get(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        if (!ExileDB.UniqueGears().isRegistered(id)) {
            return null;
        }
        return ExileDB.UniqueGears().get(id);
    }
}
