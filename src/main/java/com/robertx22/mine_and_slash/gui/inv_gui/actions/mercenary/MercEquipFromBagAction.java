package com.robertx22.mine_and_slash.gui.inv_gui.actions.mercenary;

import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.capability.player.helper.BackpackInventory;
import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.capability.player.PlayerBackpackData;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.gui.bases.GuiMousePosition;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.GuiAction;
import com.robertx22.mine_and_slash.capability.player.helper.BackpackLayouts;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercBagClientState;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenarySlotType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The master bag half of the mercenary equip picker: one entry per bag slot, alongside the player
 * inventory entries {@link MercEquipAction} provides.
 * <p>
 * Keyed by bag slot in the GUID for the same reason its sibling is keyed by inventory slot - the
 * action map is looked up by GUID on the server, so the slot has to be part of the id. Which bag tab
 * is meant follows from the mercenary slot type, which already travels as extra data, so this needs
 * no field of its own for it.
 */
public class MercEquipFromBagAction extends GuiAction<MercEquipAction.Target> {

    /**
     * Every bag slot a tab can have, so regenActionMap can cover them all. The real ceiling rather
     * than a guess: a tab is sized by datapack config, but {@code MyInventory.createTag} writes the
     * slot index as a byte, which is what caps {@code BackpackLayouts.MAX_ROWS}.
     */
    public static final int MAX_BAG_SLOTS = BackpackLayouts.MAX_ROWS * BackpackLayouts.COLUMNS;

    private final int bagSlot;

    public MercEquipFromBagAction(int bagSlot) {
        this.bagSlot = bagSlot;
    }

    /** the stack this entry stands for, out of the last snapshot the server sent. client only */
    private ItemStack cachedStack() {
        MercBagClientState state = MercBagClientState.last;
        if (state == null) {
            return ItemStack.EMPTY;
        }
        return state.getStack(MercBagClientState.tabFor(MercEquipAction.TARGET_TYPE), bagSlot);
    }

    @Override
    public ItemStack getItemStackIcon() {
        return cachedStack();
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeUtf(MercEquipAction.TARGET_TYPE.name());
        buf.writeInt(MercEquipAction.TARGET_INDEX);
    }

    @Override
    public MercEquipAction.Target loadExtraData(FriendlyByteBuf buf) {
        return new MercEquipAction.Target(buf.readUtf(), buf.readInt());
    }

    @Override
    public List<Component> getTooltip(Player p) {
        return MercEquipAction.mercTooltip(p, cachedStack());
    }

    @Override
    public void doAction(Player p, Object obj) {
        if (!(obj instanceof MercEquipAction.Target target)) {
            return;
        }
        if (!MercenaryManager.isUnlocked(p)) {
            return;
        }
        MercenarySlotType type;
        try {
            type = MercenarySlotType.valueOf(target.slotType());
        } catch (IllegalArgumentException e) {
            return;
        }

        // the bag is only usable while the master bag item is actually carried
        PlayerBackpackData cap = Load.backpacks(p);
        if (cap == null || !Backpacks.hasBackpack(p)) {
            return;
        }
        BackpackInventory bag = cap.getBackpacks().getInv(MercBagClientState.tabFor(type));
        if (bag == null || bagSlot < 0 || bagSlot >= bag.getContainerSize()) {
            return;
        }

        MercenaryData data = Load.player(p).mercs.getActive();
        MyInventory inv = type.inventoryOf(data);

        if (target.index() < 0 || target.index() >= inv.getContainerSize()) {
            return;
        }

        ItemStack stack = bag.getItem(bagSlot);
        if (stack.isEmpty()) {
            return;
        }
        // never trust the client about legality - it picked the grid contents off a snapshot, the
        // server decides against the real bag
        if (!type.mayPlace(p, data, target.index(), stack)) {
            return;
        }

        ItemStack previous = inv.getItem(target.index());

        // removeItem and not stack.split(1): split mutates the stack in place without ever telling the
        // container, so PlayerBackpackData's nbt cache would stay clean and the decrement could be
        // dropped on the next save. SimpleContainer.removeItem calls setChanged() itself, which is what
        // reaches the cache invalidation hook Backpacks.setOnChanged installed.
        ItemStack moving = bag.removeItem(bagSlot, 1);
        if (moving.isEmpty()) {
            return;
        }
        inv.setItem(target.index(), moving);

        if (!previous.isEmpty()) {
            // it came out of the bag, so what it displaced goes back to the bag rather than filling up
            // an inventory that is usually already full - forceUnequipItem would drop it at their feet.
            cap.getBackpacks().tryAutoPickup(p, previous, false);
            // emptiness, not tryAutoPickup's boolean: that returns true on a partial take, and the
            // remainder would be lost
            if (!previous.isEmpty()) {
                MercenarySlotType.giveBack(p, previous);
            }
        }

        MercenarySlotType.afterChange(p);
    }

    @Override
    public void clientAction(Player p, Object obj) {
        // keep the cursor where it is, then go back to the mercenary screen
        GuiMousePosition.save();
        ClientOnly.openMercenaryScreen();
    }

    @Override
    public String GUID() {
        return "merc_equip_bag_" + bagSlot;
    }
}
