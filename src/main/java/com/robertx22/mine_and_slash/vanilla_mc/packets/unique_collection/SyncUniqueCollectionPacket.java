package com.robertx22.mine_and_slash.vanilla_mc.packets.unique_collection;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.unique_collection.UniqueCollectionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class SyncUniqueCollectionPacket extends MyPacket<SyncUniqueCollectionPacket> {

    // registry guids are short. this only bounds a malicious/corrupt payload, it isn't a design limit
    private static final int MAX_GUID_LENGTH = 100;

    public int shards;
    public Set<String> unlocked = new HashSet<>();

    public SyncUniqueCollectionPacket() {
    }

    public SyncUniqueCollectionPacket(UniqueCollectionData data) {
        this.shards = data.shards;
        this.unlocked = new HashSet<>(data.unlocked);
    }

    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("sync_unique_collection");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        this.shards = buf.readVarInt();
        int size = buf.readVarInt();
        this.unlocked = new HashSet<>();
        for (int i = 0; i < size; i++) {
            this.unlocked.add(buf.readUtf(MAX_GUID_LENGTH));
        }
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeVarInt(this.shards);
        buf.writeVarInt(this.unlocked.size());
        for (String s : this.unlocked) {
            buf.writeUtf(s, MAX_GUID_LENGTH);
        }
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        ClientUniqueCollection.shards = this.shards;
        ClientUniqueCollection.unlocked = this.unlocked;
    }

    @Override
    public MyPacket<SyncUniqueCollectionPacket> newInstance() {
        return new SyncUniqueCollectionPacket();
    }
}
