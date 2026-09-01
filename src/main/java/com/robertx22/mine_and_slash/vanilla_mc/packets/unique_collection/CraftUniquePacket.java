package com.robertx22.mine_and_slash.vanilla_mc.packets.unique_collection;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.database.data.profession.ProfessionBlock;
import com.robertx22.mine_and_slash.database.data.profession.all.Professions;
import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import com.robertx22.mine_and_slash.database.data.unique_items.collection.BoundItemUtils;
import com.robertx22.mine_and_slash.database.data.unique_items.collection.ShardMath;
import com.robertx22.mine_and_slash.database.data.unique_items.collection.UniqueCollection;
import com.robertx22.mine_and_slash.database.data.unique_items.collection.UniqueSalvageHelper;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.unique_collection.UniqueCollectionData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * "Reconstruct this unique." Everything is re-derived and re-validated server side - the packet is only
 * ever a request, never a source of truth for the cost.
 */
public class CraftUniquePacket extends MyPacket<CraftUniquePacket> {

    private static final int MAX_GUID_LENGTH = 100;

    // the sticker book opens off the salvaging station, so the station has to still be in reach. same
    // distance vanilla containers use.
    private static final double MAX_STATION_DISTANCE_SQ = 64D;

    public String uniqueId = "";
    public BlockPos station = BlockPos.ZERO;

    public CraftUniquePacket() {
    }

    public CraftUniquePacket(String uniqueId, BlockPos station) {
        this.uniqueId = uniqueId;
        this.station = station;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("craft_unique");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        this.uniqueId = buf.readUtf(MAX_GUID_LENGTH);
        this.station = buf.readBlockPos();
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeUtf(this.uniqueId, MAX_GUID_LENGTH);
        buf.writeBlockPos(this.station);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || player.level().isClientSide) {
            return;
        }

        if (!isAtSalvagingStation(player)) {
            player.sendSystemMessage(Chats.TOO_FAR_FROM_STATION.locName().withStyle(ChatFormatting.RED));
            return;
        }

        UniqueGear unique = UniqueCollection.get(this.uniqueId);
        // isInBook is the same predicate the GUI lists by, so anything reachable through the screen
        // passes here and anything else was a forged packet
        if (unique == null || !UniqueCollection.isInBook(unique)) {
            player.sendSystemMessage(Chats.UNIQUE_NOT_CRAFTABLE.locName().withStyle(ChatFormatting.RED));
            return;
        }

        UniqueCollectionData collection = Load.player(player).uniqueCollection;

        if (!collection.isUnlocked(unique.GUID())) {
            player.sendSystemMessage(Chats.UNIQUE_NOT_UNLOCKED.locName().withStyle(ChatFormatting.RED));
            return;
        }

        int playerLevel = Load.Unit(player).getLevel();
        if (playerLevel < unique.min_drop_lvl) {
            player.sendSystemMessage(Chats.UNIQUE_LEVEL_TOO_LOW.locName(unique.min_drop_lvl).withStyle(ChatFormatting.RED));
            return;
        }

        int cost = ShardMath.craftCost(unique, playerLevel);
        if (!collection.canAfford(cost)) {
            player.sendSystemMessage(Chats.NOT_ENOUGH_SHARDS.locName(cost, collection.shards).withStyle(ChatFormatting.RED));
            return;
        }

        ItemStack stack = build(unique, player, playerLevel);

        // hand the item over BEFORE taking payment. the reverse order is how crafting profession exp
        // ended up being granted for outputs that never landed.
        if (!player.getInventory().add(stack)) {
            player.sendSystemMessage(Chats.NO_ROOM_FOR_UNIQUE.locName().withStyle(ChatFormatting.RED));
            return;
        }

        // packet handlers run sequentially on the server thread, so the affordability check above and
        // this deduct can't be interleaved by a double click
        collection.trySpend(cost);

        UniqueSalvageHelper.sync(player);
    }

    private boolean isAtSalvagingStation(Player player) {
        if (player.distanceToSqr(station.getX() + 0.5, station.getY() + 0.5, station.getZ() + 0.5) > MAX_STATION_DISTANCE_SQ) {
            return false;
        }
        return player.level().getBlockState(station).getBlock() instanceof ProfessionBlock block
                && block.profession.equals(Professions.SALVAGING);
    }

    /**
     * Item level is the player's level, not the unique's min_drop_lvl - otherwise every entry that
     * starts dropping early would be permanently stuck at that level and the book would be dead content
     * at endgame. min_drop_lvl gates whether you may craft it at all, and prices it.
     * <p>
     * level.set bypasses LevelPart's +-ITEM_LEVEL_VARIANCE roll, which matters here: a crafted item has
     * a price attached, so it must come out at exactly the level that price was quoted for.
     */
    private ItemStack build(UniqueGear unique, Player player, int playerLevel) {
        GearBlueprint blueprint = new GearBlueprint(LootInfo.ofLevel(playerLevel));
        blueprint.level.set(playerLevel);
        blueprint.rarity.set(ExileDB.GearRarities().get(unique.rarity));
        blueprint.uniquePart.set(unique);
        blueprint.gearItemSlot.set(unique.getBaseGear());

        return BoundItemUtils.bindTo(blueprint.createStack(), player);
    }

    @Override
    public MyPacket<CraftUniquePacket> newInstance() {
        return new CraftUniquePacket();
    }
}
