package com.robertx22.mine_and_slash.database.data.unique_items.collection;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.database.data.profession.all.Professions;
import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.saveclasses.unique_collection.UniqueCollectionData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.vanilla_mc.packets.unique_collection.SyncUniqueCollectionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;

/**
 * Everything that happens to the sticker book when a unique is destroyed.
 * <p>
 * Called from both salvage paths - the station and auto salvage - because the payout has to be the same
 * either way, and because the unlock is the only way an entry ever enters the book.
 */
public class UniqueSalvageHelper {

    /**
     * @return the unique the stack represents, or null if it isn't a unique or its entry is retired.
     */
    public static UniqueGear resolveUnique(ExileStack ex) {
        if (!ex.get(StackKeys.CUSTOM).has()) {
            return null;
        }
        String id = ex.get(StackKeys.CUSTOM).getOrCreate().data.get(CustomItemData.KEYS.UNIQUE_ID);
        UniqueGear unique = UniqueCollection.get(id);
        if (unique == null || unique.weight <= 0) {
            // a retired entry still resolves so old stacks keep working, but it has no meaningful
            // weight to price against, so it pays nothing rather than something arbitrary
            return null;
        }
        return unique;
    }

    /**
     * Grants shards and, when the unique belongs in the book, records the unlock.
     * <p>
     * Called exactly once per item. It deliberately does NOT ride the station's extraSalvageChance
     * loop - that keeps multiplying rarity stones only. The salvaging profession feeds shards through
     * the base constant instead, which is a far smaller swing than doubling every payout.
     *
     * @param announce whether to tell the player the shard count. The station does; auto salvage
     *                 doesn't, or bulk pickup would flood chat. A first time unlock always announces.
     */
    public static void onUniqueSalvaged(Player player, ExileStack ex, GearItemData gear, boolean announce) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        UniqueGear unique = resolveUnique(ex);
        if (unique == null) {
            return;
        }

        UniqueCollectionData collection = Load.player(player).uniqueCollection;

        int profLevel = Load.player(player).professions.getLevel(Professions.SALVAGING);
        int shards = ShardMath.salvageShards(unique, gear.getLevel(), profLevel, player.level().random.nextDouble());
        collection.addShards(shards);

        // A reconstructed unique is account bound, and binding only ever suppressed STATS
        // (GearData.isUsableBy) - it did nothing to salvaging. So a player with shards could craft
        // copies of a rare entry, hand them out, and every recipient unlocked it permanently by
        // salvaging them: book completion became something one rich player could gift to a server.
        //
        // Shards are still paid, because the item was genuinely destroyed. Only the unlock is
        // refused. A found or traded unique carries no binding at all, so ordinary trading is
        // untouched, and salvaging your OWN craft back still behaves exactly as before.
        String owner = BoundItemUtils.getOwnerUuid(ex.getStack());
        boolean someoneElsesCraft = !owner.isEmpty() && !owner.equals(player.getStringUUID());

        // league, hidden and retired uniques pay shards but never unlock - they aren't craftable, and
        // an unlock for an entry the book won't render would be invisible dead state
        boolean newUnlock = !someoneElsesCraft
                && UniqueCollection.isInBook(unique)
                && collection.unlock(unique.GUID());

        if (announce) {
            player.sendSystemMessage(Chats.SHARDS_GAINED.locName(shards).withStyle(ChatFormatting.AQUA));
        }
        if (newUnlock) {
            player.sendSystemMessage(Chats.UNIQUE_UNLOCKED.locName(unique.locName()).withStyle(ChatFormatting.GOLD));
        }

        sync(player);
    }

    public static void sync(Player player) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        // setDirty is what gets the collection written back to disk - the packet only feeds the client
        // mirror, since the collection deliberately isn't part of the PlayerData sync tag.
        Load.player(player).playerDataSync.setDirty();
        Packets.sendToClient(player, new SyncUniqueCollectionPacket(Load.player(player).uniqueCollection));
    }
}
