package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.gear_types.bases.BaseGearType;
import com.robertx22.mine_and_slash.database.data.stats.types.offense.DualWieldEffectiveness;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.tags.all.SlotTags;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.enumclasses.WeaponTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class DualWieldUtils {

    // a weapon that's allowed to sit in the offhand. same check GearData uses to decide that the
    // offhand only grants a share of its stats.
    public static boolean isDualWieldWeapon(GearItemData gear) {
        if (gear == null) {
            return false;
        }
        BaseGearType type = gear.GetBaseGearType();
        if (type == null || !type.getTags().contains(SlotTags.weapon_family)) {
            return false;
        }
        WeaponTypes wep = type.weaponType();
        return wep != null && wep.can_dual_wield;
    }

    public static boolean isDualWieldWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return isDualWieldWeapon(StackSaving.GEARS.loadFrom(stack));
    }

    // two handed weapons are simply the ones that can't be dual wielded. the pack uses
    // can_dual_wield as its 1h/2h flag: axe/dagger/gauntlet/hammer/staff/sword are one handed,
    // greatsword/scythe/spear/trident/bow/crossbow are two handed.
    public static boolean isTwoHandedWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        GearItemData gear = StackSaving.GEARS.loadFrom(stack);
        if (gear == null) {
            return false;
        }
        BaseGearType type = gear.GetBaseGearType();
        if (type == null || !type.getTags().contains(SlotTags.weapon_family)) {
            return false;
        }
        WeaponTypes wep = type.weaponType();
        return wep != null && !wep.can_dual_wield;
    }

    // a two handed weapon occupies both hands, so a weapon in the offhand alongside one isn't
    // wielded at all and grants nothing. offhand_family items (shield/tome/totem) are unaffected.
    public static boolean mainHandBlocksOffhandWeapon(LivingEntity en) {
        return en != null && isTwoHandedWeapon(en.getMainHandItem());
    }

    // dual wielding is a one hand weapon in *each* hand. since a two handed mainhand now blocks
    // the offhand weapon outright, this is the same condition as "the offhand weapon grants stats".
    public static boolean isDualWielding(LivingEntity en) {
        if (en == null) {
            return false;
        }
        return isDualWieldWeapon(en.getMainHandItem()) && isDualWieldWeapon(en.getOffhandItem());
    }

    // the config's 25% share, scaled by Dual-Wield Effectiveness. the stat multiplies the share
    // rather than adding to it, so 10% effectiveness turns 25% into 27.5%, not 35%.
    public static float getOffhandStatPercent(EntityData data) {
        return ServerContainer.get().PERC_OFFHAND_WEP_STAT.get() * getEffectivenessMulti(data);
    }

    public static float getEffectiveness(EntityData data) {
        if (data == null || data.getUnit() == null) {
            return 0;
        }
        return data.getUnit().getCalculatedStat(DualWieldEffectiveness.getInstance()).getValue();
    }

    public static float getEffectivenessMulti(EntityData data) {
        return Math.max(0, 1F + getEffectiveness(data) / 100F);
    }
}
