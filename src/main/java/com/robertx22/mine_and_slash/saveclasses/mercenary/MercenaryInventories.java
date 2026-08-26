package com.robertx22.mine_and_slash.saveclasses.mercenary;

import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Arrays;
import java.util.List;

/**
 * Sizes and slot indices for the three inventories a mercenary owns. One place so the gui, the
 * persistence and the equip actions can't disagree about which index is the offhand.
 */
public class MercenaryInventories {

    // the order the gear inventory is laid out in. index in this list == slot index in the inventory.
    public static final List<EquipmentSlot> GEAR_SLOTS = Arrays.asList(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND);

    public static final int GEAR_SIZE = GEAR_SLOTS.size();
    public static final int SUPPORT_SIZE = MercenaryClass.EQUIPPED_SKILLS * MercenaryClass.SUPPORTS_PER_SKILL;
    public static final int AURA_SIZE = MercenaryClass.AURA_SLOTS;

    public static MyInventory newGear() {
        return new MyInventory(GEAR_SIZE);
    }

    public static MyInventory newSupports() {
        return new MyInventory(SUPPORT_SIZE);
    }

    public static MyInventory newAuras() {
        return new MyInventory(AURA_SIZE);
    }

    public static int gearIndexOf(EquipmentSlot slot) {
        return GEAR_SLOTS.indexOf(slot);
    }

    public static EquipmentSlot gearSlotAt(int index) {
        if (index < 0 || index >= GEAR_SIZE) {
            return null;
        }
        return GEAR_SLOTS.get(index);
    }

    /** support slot {@code support} (0 based) of equipped skill {@code skill} */
    public static int supportIndex(int skill, int support) {
        return skill * MercenaryClass.SUPPORTS_PER_SKILL + support;
    }
}
