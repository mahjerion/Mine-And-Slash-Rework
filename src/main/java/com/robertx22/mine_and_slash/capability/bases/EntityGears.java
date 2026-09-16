package com.robertx22.mine_and_slash.capability.bases;

import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;

public class EntityGears {

    private HashMap<EquipmentSlot, ItemStack> map = new HashMap<>();

    // null means "never observed". a fresh capability - login, respawn and dimension changes all
    // build a new player entity and a new EntityGears - must not read as a weapon swap, or the
    // player spawns in locked out of their own hotbar
    private Item lastMainHandItem = null;
    private String lastMainHandGearType = "";

    /**
     * Records the main hand and answers whether its <i>casting identity</i> moved - the item itself,
     * or the Mine and Slash base gear type, which is what CastingWeapon's predicates read. Two stacks
     * agreeing on both are interchangeable for every gate the weapon swap lockout protects.
     * <p>
     * Deliberately not stack identity: a mod handing back a fresh copy of the same stack every tick
     * would read as a swap forever. Deliberately not full nbt either - that moves on every point of
     * durability, and casting damages the weapon. Returns false the first time it is ever called.
     */
    public boolean observeMainHand(ItemStack now) {

        Item item = (now == null || now.isEmpty()) ? Items.AIR : now.getItem();

        String gtype = "";
        if (now != null && !now.isEmpty() && StackSaving.GEARS.has(now)) { // cheap nbt key check first
            GearItemData gear = StackSaving.GEARS.loadFrom(now);
            if (gear != null) {
                gtype = gear.gtype;
            }
        }

        boolean known = lastMainHandItem != null;
        boolean changed = known && (item != lastMainHandItem || !gtype.equals(lastMainHandGearType));

        lastMainHandItem = item;
        lastMainHandGearType = gtype;

        return changed;
    }

    public ItemStack get(EquipmentSlot slot) {
        if (map.isEmpty()) {
            for (EquipmentSlot s : EquipmentSlot.values()) {
                map.put(s, ItemStack.EMPTY);
            }
        }
        return map.get(slot);
    }

    public ItemStack put(EquipmentSlot slot, ItemStack stack) {
        if (map.isEmpty()) {
            for (EquipmentSlot s : EquipmentSlot.values()) {
                map.put(s, ItemStack.EMPTY);
            }
        }
        return map.put(slot, stack);
    }

}
