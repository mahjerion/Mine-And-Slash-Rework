package com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.capability.player.PlayerBackpackData;
import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.capability.player.helper.BackpackInventory;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;
import com.robertx22.mine_and_slash.tags.all.SlotTags;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and sends the master bag snapshot the mercenary equip picker reads. Server side only - the
 * client half is {@link MercBagClientState}.
 */
public class MercBagServer {

    public static void sync(Player p) {
        // sendToClient no-ops for anything that isn't a ServerPlayer, so this is safe to call from
        // shared code like MercenarySlotType.afterChange
        Packets.sendToClient(p, new MercBagSyncPacket(snapshot(p)));
    }

    /**
     * The bag's gear and skill gem tabs, minus everything no mercenary slot could ever take.
     * <p>
     * The prefilter is deliberately a coarse superset of {@code MercenarySlotType.mayPlace} and reads
     * nothing about the mercenary itself. Filtering by the real rule here would be wrong as well as
     * tempting: a one hander is illegal while a two hander is equipped, so it would drop out of the
     * snapshot and then stay missing from the picker after the two hander came off. The client runs
     * the real per-slot check when it builds the grid; this only keeps tens of kilobytes of gear nbt
     * off the wire.
     */
    public static MercBagClientState snapshot(Player p) {
        MercBagClientState state = new MercBagClientState();

        PlayerBackpackData cap = Load.backpacks(p);
        if (cap == null || !Backpacks.hasBackpack(p)) {
            return state;
        }
        state.hasBackpack = true;

        for (Backpacks.BackpackType tab : MercBagClientState.syncedTabs()) {
            BackpackInventory inv = cap.getBackpacks().getInv(tab);
            if (inv == null) {
                continue;
            }
            List<MercBagClientState.Entry> entries = new ArrayList<>();

            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty() || !couldEverFit(tab, stack)) {
                    continue;
                }
                entries.add(new MercBagClientState.Entry(i, stack.copy()));
            }
            state.byTab.put(tab, entries);
        }
        return state;
    }

    private static boolean couldEverFit(Backpacks.BackpackType tab, ItemStack stack) {
        if (tab == Backpacks.BackpackType.GEARS) {
            // the tab also holds jewels and stat souls, and a mercenary wears no jewellery
            GearItemData gear = StackSaving.GEARS.loadFrom(stack);
            if (gear == null || !gear.isValidItem() || gear.GetBaseGearType() == null) {
                return false;
            }
            return !gear.GetBaseGearType().getTags().contains(SlotTags.jewelry_family);
        }
        if (tab == Backpacks.BackpackType.SKILL_GEMS) {
            // skill gems themselves are picked from the mercenary's learned list, not from the bag
            SkillGemData gem = StackSaving.SKILL_GEM.loadFrom(stack);
            return gem != null && (gem.type == SkillGemData.SkillGemType.SUPPORT || gem.type == SkillGemData.SkillGemType.AURA);
        }
        return false;
    }
}
