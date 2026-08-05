package com.robertx22.mine_and_slash.capability.player;

import com.robertx22.mine_and_slash.capability.CapNbtCache;
import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.library_of_exile.components.ICap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerBackpackData implements ICap {


    public static final ResourceLocation RESOURCE = new ResourceLocation(SlashRef.MODID, "backpacks");
    public static Capability<PlayerBackpackData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static PlayerBackpackData get(LivingEntity entity) {
        return entity.getCapability(INSTANCE).orElse(null);
    }

    transient final LazyOptional<PlayerBackpackData> supp = LazyOptional.of(() -> this);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == INSTANCE) {
            return supp.cast();
        }
        return LazyOptional.empty();

    }


    transient Player player;
    private Backpacks data;

    // 5 tabs of 54 slots is 270 ItemStack.save calls, which anything reading the player's nbt was
    // paying for every tick. backpacks are never sent to the client (see syncToClient below), so a
    // stale tag here could only ever reach a save, and CapNbtCache.invalidate is forced before those.
    private transient final CapNbtCache nbtCache = new CapNbtCache();

    public PlayerBackpackData(Player player) {
        this.player = player;
        this.data = new Backpacks(player);

        for (Backpacks.BackpackType type : Backpacks.BackpackType.values()) {
            data.getInv(type).onChanged(nbtCache::markDirty);
        }
    }

    public Backpacks getBackpacks() {
        return data;
    }

    public CapNbtCache getNbtCache() {
        return nbtCache;
    }


    @Override
    public CompoundTag serializeNBT() {
        return nbtCache.get(player, this::buildNBT);
    }

    private CompoundTag buildNBT() {

        CompoundTag nbt = new CompoundTag();

        for (Backpacks.BackpackType type : Backpacks.BackpackType.values()) {
            try {
                nbt.put(type.id, data.getInv(type).createTag());
            } catch (Exception e) {
                // throw new RuntimeException(e);
            }
        }

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        // anything cached was built before this data existed. fromTag marks dirty through setChanged
        // for every tab it actually loads, but a tab missing from the nbt never gets there.
        nbtCache.markDirty();

        for (Backpacks.BackpackType type : Backpacks.BackpackType.values()) {
            try {
                if (nbt.contains(type.id)) {
                    data.getInv(type).fromTag(nbt.getList(type.id, 10)); // todo
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void syncToClient(Player player) {
        // dont sync backpacks to client
        //  Packets.sendToClient(player, new SyncPlayerCapToClient(player, this.getCapIdForSyncing()));
    }

    public static final String ID = "backpack_data";
    @Override
    public String getCapIdForSyncing() {
        return ID;
    }

}
