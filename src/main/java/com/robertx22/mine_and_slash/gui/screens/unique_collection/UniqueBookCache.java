package com.robertx22.mine_and_slash.gui.screens.unique_collection;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import com.robertx22.mine_and_slash.database.data.unique_items.collection.UniqueCollection;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.library_of_exile.utils.CLOC;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The rendered book, cached.
 * <p>
 * Rebuilt when the player's level changes, because the preview is rolled at that level and is meant to
 * show what reconstructing would actually hand you. Also cleared on a datapack reload, since the whole
 * entry list comes out of the registry.
 */
public class UniqueBookCache {

    private static List<UniqueBookEntry> cached = null;
    private static int cachedLevel = -1;

    static {
        ExileEvents.ON_REGISTER_TO_DATABASE.register(new EventConsumer<>() {
            @Override
            public void accept(ExileEvents.OnRegisterToDatabase e) {
                clear();
            }
        });
    }

    public static void clear() {
        cached = null;
        cachedLevel = -1;
    }

    public static List<UniqueBookEntry> get(int playerLevel) {
        if (cached != null && cachedLevel == playerLevel) {
            return cached;
        }
        List<UniqueBookEntry> list = new ArrayList<>();
        for (UniqueGear unique : UniqueCollection.all()) {
            ItemStack preview = buildPreview(unique, playerLevel);
            if (preview == null || preview.isEmpty()) {
                continue;
            }
            String name = CLOC.translate(unique.locName());
            list.add(new UniqueBookEntry(unique, preview, name.toLowerCase(Locale.ROOT)));
        }
        // an empty result means the unique registry hasn't synced yet, not that the book is empty.
        // caching it would leave the screen blank for the rest of the session.
        if (!list.isEmpty()) {
            cached = list;
            cachedLevel = playerLevel;
        }
        return list;
    }

    private static ItemStack buildPreview(UniqueGear unique, int playerLevel) {
        try {
            GearRarity rarity = ExileDB.GearRarities().get(unique.rarity);
            if (rarity == null) {
                return ItemStack.EMPTY;
            }
            // same path CraftUniquePacket uses, so the preview can't drift from what you get
            GearBlueprint blueprint = new GearBlueprint(LootInfo.ofLevel(playerLevel));
            blueprint.level.set(playerLevel);
            blueprint.rarity.set(rarity);
            blueprint.uniquePart.set(unique);
            blueprint.gearItemSlot.set(unique.getBaseGear());
            return blueprint.createStack();
        } catch (Exception e) {
            // a datapack can point base_gear at an item that isn't loaded. one broken entry shouldn't
            // take the whole screen down with it.
            e.printStackTrace();
            return ItemStack.EMPTY;
        }
    }
}
