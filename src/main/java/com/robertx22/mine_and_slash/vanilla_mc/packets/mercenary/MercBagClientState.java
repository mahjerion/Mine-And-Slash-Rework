package com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary;

import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.BackpackItemSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The master bag contents the mercenary equip picker is allowed to offer, as sent by the server.
 * <p>
 * Backpacks are deliberately never synced ({@code PlayerBackpackData.syncToClient} is an empty
 * method), and the picker grid is built client side, so without this the bag may as well not exist
 * as far as the mercenary screen is concerned. Same answer the map device reached for the same
 * reason - see {@code MapDeviceClientState}: the screen renders a server pushed snapshot, and the
 * server re-reads the real container before acting on anything the client picked.
 * <p>
 * Plain POJO on purpose. It is a packet field type, so it loads on the dedicated server too and must
 * not touch anything client only.
 */
public class MercBagClientState {

    /** the most recent snapshot this client received */
    public static MercBagClientState last = null;

    public record Entry(int bagSlot, ItemStack stack) {
    }

    /** false when the player isn't carrying the bag at all, which hides every entry */
    public boolean hasBackpack = false;

    public Map<Backpacks.BackpackType, List<Entry>> byTab = new EnumMap<>(Backpacks.BackpackType.class);

    /** the bag tab a mercenary slot draws from */
    public static Backpacks.BackpackType tabFor(MercenarySlotType type) {
        return type == MercenarySlotType.GEAR ? Backpacks.BackpackType.GEARS : Backpacks.BackpackType.SKILL_GEMS;
    }

    /** the tabs a snapshot carries - the only two any mercenary slot can pull from */
    public static List<Backpacks.BackpackType> syncedTabs() {
        return List.of(Backpacks.BackpackType.GEARS, Backpacks.BackpackType.SKILL_GEMS);
    }

    public List<Entry> get(Backpacks.BackpackType tab) {
        if (!hasBackpack) {
            return List.of();
        }
        return byTab.getOrDefault(tab, List.of());
    }

    public ItemStack getStack(Backpacks.BackpackType tab, int bagSlot) {
        for (Entry e : get(tab)) {
            if (e.bagSlot() == bagSlot) {
                return e.stack();
            }
        }
        return ItemStack.EMPTY;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(hasBackpack);

        List<Backpacks.BackpackType> tabs = syncedTabs();
        buf.writeVarInt(tabs.size());

        for (Backpacks.BackpackType tab : tabs) {
            buf.writeEnum(tab);

            List<Entry> entries = byTab.getOrDefault(tab, List.of());
            buf.writeVarInt(entries.size());

            for (Entry e : entries) {
                buf.writeVarInt(e.bagSlot());
                // a bag tab can have a datapack stack_multiplier, so a count is not guaranteed to fit
                // the byte vanilla's writeItem uses. the backpack packets already write through this
                BackpackItemSerializer.writeItem(buf, e.stack());
            }
        }
    }

    public static MercBagClientState read(FriendlyByteBuf buf) {
        MercBagClientState s = new MercBagClientState();
        s.hasBackpack = buf.readBoolean();

        int tabCount = buf.readVarInt();
        for (int t = 0; t < tabCount; t++) {
            Backpacks.BackpackType tab = buf.readEnum(Backpacks.BackpackType.class);

            int size = buf.readVarInt();
            List<Entry> entries = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                int slot = buf.readVarInt();
                entries.add(new Entry(slot, BackpackItemSerializer.readItem(buf)));
            }
            s.byTab.put(tab, entries);
        }
        return s;
    }
}
