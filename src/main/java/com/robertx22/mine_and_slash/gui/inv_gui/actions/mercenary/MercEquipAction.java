package com.robertx22.mine_and_slash.gui.inv_gui.actions.mercenary;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.gui.bases.GuiMousePosition;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.GuiAction;
import com.robertx22.mine_and_slash.gui.screens.mercenary.MercenaryScreen;
import com.robertx22.mine_and_slash.database.data.mercenary.ClientMercenary;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearTooltipUtils;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenarySlotType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry in the "pick something for this slot" grid the mercenary screen opens when a slot is
 * clicked. The design offered either a chest style inventory row or a picker; this is the picker,
 * which keeps the screen at the mocked up 256x256.
 * <p>
 * The action map is keyed by GUID, so the player inventory slot has to travel in the id - one action
 * per inventory slot, registered up front. Which mercenary slot it is going into travels as extra
 * data, the same way {@link com.robertx22.mine_and_slash.gui.inv_gui.actions.PickSpellAction} carries
 * its hotbar index.
 */
public class MercEquipAction extends GuiAction<MercEquipAction.Target> {

    /** every slot a player inventory can have, so regenActionMap can cover them all */
    public static final int MAX_INVENTORY_SLOTS = 41;

    /**
     * Whether an inventory slot is one the player can hand to a mercenary.
     * <p>
     * A vanilla {@code Inventory} indexes 0-35 as the backpack and hotbar, 36-39 as the armour
     * actually being worn and 40 as the offhand, and {@code getItem} maps into all three lists
     * transparently. So the picker was offering the player's own equipped armour and shield, and
     * taking one stripped it off them mid fight. The held weapon is excluded for the same reason -
     * and only the held one, so switching hotbar slot makes the previous weapon offerable again.
     * <p>
     * Shared by the grid builder and by {@code doAction}, so the client filter and the server's
     * final say cannot drift apart.
     */
    public static boolean isOfferableSlot(Player p, int invSlot) {
        if (invSlot < 0 || invSlot >= net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE) {
            return false;
        }
        return invSlot != p.getInventory().selected;
    }

    public record Target(String slotType, int index) {
    }

    // set by the screen just before the grid is built, read by saveExtraData. same one-shot static as
    // PickSpellAction.SLOT - the grid is constructed and sent in the same tick as the click.
    public static MercenarySlotType TARGET_TYPE = MercenarySlotType.GEAR;
    public static int TARGET_INDEX = 0;

    private final int invSlot;

    public MercEquipAction(int invSlot) {
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
        buf.writeUtf(TARGET_TYPE.name());
        buf.writeInt(TARGET_INDEX);
    }

    @Override
    public Target loadExtraData(FriendlyByteBuf buf) {
        return new Target(buf.readUtf(), buf.readInt());
    }

    @Override
    public List<Component> getTooltip(Player p) {
        List<Component> list = new ArrayList<>();
        ItemStack stack = p.getInventory().getItem(invSlot);
        if (stack.isEmpty()) {
            return list;
        }

        // this grid only ever lists gear the MERCENARY can wear - mayPlace runs meetsAttributeReq
        // against its stats - so the requirement check marks have to be judged the same way, or every
        // entry the owner personally falls short of is offered and then marked with a red X.
        MercenaryEntity merc = ClientMercenary.get();

        GearTooltipUtils.TOOLTIP_ENTITY_OVERRIDE = merc == null ? null : Load.Unit(merc);
        try {
            list.addAll(stack.getTooltipLines(p, TooltipFlag.NORMAL));
        } finally {
            // a leak here would write the next tooltip drawn this frame for the mercenary too
            GearTooltipUtils.TOOLTIP_ENTITY_OVERRIDE = null;
        }
        return list;
    }

    @Override
    public void doAction(Player p, Object obj) {
        if (!(obj instanceof Target target)) {
            return;
        }
        MercenarySlotType type;
        try {
            type = MercenarySlotType.valueOf(target.slotType());
        } catch (IllegalArgumentException e) {
            return;
        }

        MercenaryData data = Load.player(p).mercs.getActive();
        MyInventory inv = type.inventoryOf(data);

        if (target.index() < 0 || target.index() >= inv.getContainerSize()) {
            return;
        }

        // the grid already filters these out, but the action map has one entry per inventory slot
        // and a packet naming a worn slot would otherwise strip the player's own armour
        if (!isOfferableSlot(p, invSlot)) {
            return;
        }

        ItemStack stack = p.getInventory().getItem(invSlot);
        if (stack.isEmpty()) {
            return;
        }
        // never trust the client about legality - it picked the grid contents, the server decides
        if (!type.mayPlace(p, data, target.index(), stack)) {
            return;
        }

        // one item per slot: whatever was there goes back to the player before the new one lands, so
        // a swap can never duplicate or eat a stack.
        ItemStack previous = inv.getItem(target.index());
        ItemStack moving = stack.split(1);

        inv.setItem(target.index(), moving);
        if (!previous.isEmpty()) {
            MercenarySlotType.giveBack(p, previous);
        }

        MercenarySlotType.afterChange(p);
    }

    @Override
    public void clientAction(Player p, Object obj) {
        // keep the cursor where it is, then go back to the mercenary screen
        GuiMousePosition.save();
        Minecraft.getInstance().setScreen(new MercenaryScreen());
    }

    @Override
    public String GUID() {
        return "merc_equip_" + invSlot;
    }
}
