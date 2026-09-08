package com.robertx22.mine_and_slash.gui.inv_gui.actions.map_device;

import com.robertx22.addons.map_device.MapDeviceServer;
import com.robertx22.mine_and_slash.gui.bases.GuiMousePosition;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.GuiAction;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry in the "pick something for this slot" grid the map device screen opens when a slot is
 * clicked - the same picker the mercenary screen uses ({@code MercEquipAction}).
 * <p>
 * The action map is keyed by GUID, so the player inventory slot travels in the id - one action per
 * inventory slot, registered up front in {@link GuiAction#regenActionMap()}. Which device and which of
 * its slots the item is going into travels as extra data.
 */
public class MapDeviceEquipAction extends GuiAction<MapDeviceEquipAction.Target> {

    /**
     * Only the main inventory and hotbar (0-35): armour being worn and the offhand are never offered.
     * Unlike the mercenary picker the selected hotbar slot IS included - that is where the map the
     * player just walked up with usually sits.
     */
    public static final int MAX_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    public static boolean isOfferableSlot(int invSlot) {
        return invSlot >= 0 && invSlot < MAX_INVENTORY_SLOTS;
    }

    public record Target(BlockPos pos, int slot) {
    }

    // set by the screen just before the grid is built, read by saveExtraData. same one-shot static as
    // MercEquipAction.TARGET_* - the grid is constructed and sent in the same tick as the click.
    public static BlockPos TARGET_POS = BlockPos.ZERO;
    public static int TARGET_SLOT = 0;

    private final int invSlot;

    public MapDeviceEquipAction(int invSlot) {
        this.invSlot = invSlot;
    }

    @Override
    public ItemStack getItemStackIcon() {
        // client side only - the grid is built from the local player's inventory
        Player p = ClientOnly.getPlayer();
        if (p == null) {
            return ItemStack.EMPTY;
        }
        return p.getInventory().getItem(invSlot);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(TARGET_POS);
        buf.writeVarInt(TARGET_SLOT);
    }

    @Override
    public Target loadExtraData(FriendlyByteBuf buf) {
        return new Target(buf.readBlockPos(), buf.readVarInt());
    }

    @Override
    public List<Component> getTooltip(Player p) {
        List<Component> list = new ArrayList<>();
        ItemStack stack = p.getInventory().getItem(invSlot);
        if (!stack.isEmpty()) {
            list.addAll(stack.getTooltipLines(p, TooltipFlag.NORMAL));
        }
        return list;
    }

    @Override
    public void doAction(Player p, Object obj) {
        if (!(obj instanceof Target target)) {
            return;
        }
        // the grid already filters these out, but the action map has one entry per inventory slot and a
        // packet naming a worn slot would otherwise strip the player's own armour
        if (!isOfferableSlot(invSlot)) {
            return;
        }
        var device = MapDeviceServer.resolve(p, target.pos());
        if (device == null) {
            return;
        }
        // never trust the client about legality - it picked the grid contents, the server decides
        MapDeviceServer.equip(p, device, target.pos(), invSlot, target.slot());
    }

    @Override
    public void clientAction(Player p, Object obj) {
        // keep the cursor where it is, then go back to the device screen. the snapshot it shows is the
        // one from before the pick; the server's reply refreshes it a moment later
        GuiMousePosition.save();
        ClientOnly.openMapDeviceScreenOrClose();
    }

    @Override
    public String GUID() {
        return "map_device_equip_" + invSlot;
    }
}
