package com.robertx22.addons.map_device;

import com.robertx22.library_of_exile.dimension.device.IMapDeviceBlockEntity;
import com.robertx22.library_of_exile.dimension.device.MapDeviceKind;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the map device screen shows, as sent by the server. Block entities are not synced to
 * clients, so the screen never reads the block entity itself - it renders this snapshot, and the server
 * sends a fresh one after every change it makes to the device.
 */
public class MapDeviceClientState {

    /** the most recent snapshot this client received, so the picker can return to the device screen */
    public static MapDeviceClientState last = null;

    public BlockPos pos = BlockPos.ZERO;
    public MapDeviceKind kind = MapDeviceKind.DUNGEON;

    public boolean activated = false;
    public boolean hasMapSlot = true;
    public boolean freeRunAvailable = false;

    public boolean showTickets = false;
    public boolean unlimitedTickets = false;
    public int ticketsLeft = 0;
    public int ticketsMax = 0;

    public List<ItemStack> stacks = new ArrayList<>();

    public MapDeviceClientState() {
        for (int i = 0; i < IMapDeviceBlockEntity.SIZE; i++) {
            stacks.add(ItemStack.EMPTY);
        }
    }

    public ItemStack getStack(int slot) {
        return slot >= 0 && slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY;
    }

    public ItemStack getMapStack() {
        return getStack(IMapDeviceBlockEntity.MAP_SLOT);
    }

    public boolean hasAnyRelic() {
        for (int i = IMapDeviceBlockEntity.RELIC_SLOT_START; i < IMapDeviceBlockEntity.SIZE; i++) {
            if (!getStack(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** whether the action button does anything: a map is slotted, a free run is waiting, or a run is live */
    public boolean canStartOrJoin() {
        return activated || freeRunAvailable || !getMapStack().isEmpty();
    }

    /** true when pressing the button starts a NEW run rather than joining the bound one */
    public boolean wouldStartNew() {
        return freeRunAvailable || !getMapStack().isEmpty();
    }

    /** a throwaway container with the same contents, for the shared relic rules that read a Container */
    public SimpleContainer asContainer() {
        SimpleContainer c = new SimpleContainer(IMapDeviceBlockEntity.SIZE);
        for (int i = 0; i < IMapDeviceBlockEntity.SIZE; i++) {
            c.setItem(i, getStack(i).copy());
        }
        return c;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(kind);
        buf.writeBoolean(activated);
        buf.writeBoolean(hasMapSlot);
        buf.writeBoolean(freeRunAvailable);
        buf.writeBoolean(showTickets);
        buf.writeBoolean(unlimitedTickets);
        buf.writeVarInt(ticketsLeft);
        buf.writeVarInt(ticketsMax);
        buf.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            buf.writeItem(stack);
        }
    }

    public static MapDeviceClientState read(FriendlyByteBuf buf) {
        MapDeviceClientState s = new MapDeviceClientState();
        s.pos = buf.readBlockPos();
        s.kind = buf.readEnum(MapDeviceKind.class);
        s.activated = buf.readBoolean();
        s.hasMapSlot = buf.readBoolean();
        s.freeRunAvailable = buf.readBoolean();
        s.showTickets = buf.readBoolean();
        s.unlimitedTickets = buf.readBoolean();
        s.ticketsLeft = buf.readVarInt();
        s.ticketsMax = buf.readVarInt();
        int size = buf.readVarInt();
        s.stacks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            s.stacks.add(buf.readItem());
        }
        while (s.stacks.size() < IMapDeviceBlockEntity.SIZE) {
            s.stacks.add(ItemStack.EMPTY);
        }
        return s;
    }
}
