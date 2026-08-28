package com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary;

import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.database.data.gear_types.bases.BaseGearType;
import com.robertx22.mine_and_slash.database.data.mercenary.ClientMercenary;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryInventories;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;
import com.robertx22.mine_and_slash.tags.all.SlotTags;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.stat_calculation.MercenaryStatUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.DualWieldUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The three inventories a mercenary slot can belong to, plus the legality rules for each. Kept in one
 * place so the picker grid, the equip action and the unequip packet can't disagree about what fits.
 */
public enum MercenarySlotType {

    GEAR {
        @Override
        public MyInventory inventoryOf(MercenaryData data) {
            return data.getGear();
        }

        @Override
        public boolean mayPlace(Player owner, MercenaryData data, int index, ItemStack stack) {
            EquipmentSlot slot = MercenaryInventories.gearSlotAt(index);
            if (slot == null) {
                return false;
            }
            GearItemData gear = StackSaving.GEARS.loadFrom(stack);
            if (gear == null || !gear.isValidItem()) {
                return false;
            }
            // jewellery is explicitly off limits (design section 4)
            if (gear.GetBaseGearType() != null && gear.GetBaseGearType().getTags().contains(SlotTags.jewelry_family)) {
                return false;
            }
            if (gear.getLevel() > data.lvl) {
                return false;
            }
            if (!meetsAttributeReq(owner, gear)) {
                return false;
            }
            if (!fitsSlot(gear, slot)) {
                return false;
            }
            // a two hander occupies both hands, so nothing may sit alongside it, and it may not be
            // put on while something is already in the offhand.
            if (slot == EquipmentSlot.OFFHAND) {
                ItemStack main = data.getGear().getItem(MercenaryInventories.gearIndexOf(EquipmentSlot.MAINHAND));
                if (DualWieldUtils.isTwoHandedWeapon(main)) {
                    return false;
                }
            }
            if (slot == EquipmentSlot.MAINHAND && DualWieldUtils.isTwoHandedWeapon(stack)) {
                ItemStack off = data.getGear().getItem(MercenaryInventories.gearIndexOf(EquipmentSlot.OFFHAND));
                if (!off.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    },

    SUPPORT {
        @Override
        public MyInventory inventoryOf(MercenaryData data) {
            return data.getSupports();
        }

        @Override
        public boolean mayPlace(Player owner, MercenaryData data, int index, ItemStack stack) {
            SkillGemData gem = StackSaving.SKILL_GEM.loadFrom(stack);
            if (gem == null || gem.type != SkillGemData.SkillGemType.SUPPORT) {
                return false;
            }
            if (gem.getGeneric() != null && gem.getGeneric().getRequiredLevel() > data.lvl) {
                return false;
            }
            // the slot has to actually be unlocked. support slots open one per 5 levels after the
            // skill they sit under unlocks, up to 3 (design section 6).
            int skill = index / MercenaryClass.SUPPORTS_PER_SKILL;
            int within = index % MercenaryClass.SUPPORTS_PER_SKILL;
            return within < data.getSupportSlots(skill);
        }
    },

    AURA {
        @Override
        public MyInventory inventoryOf(MercenaryData data) {
            return data.getAuras();
        }

        @Override
        public boolean mayPlace(Player owner, MercenaryData data, int index, ItemStack stack) {
            SkillGemData gem = StackSaving.SKILL_GEM.loadFrom(stack);
            if (gem == null || gem.type != SkillGemData.SkillGemType.AURA || gem.getAura() == null) {
                return false;
            }
            if (gem.getAura().min_lvl > data.lvl) {
                return false;
            }
            // no running the same aura twice
            for (SkillGemData other : MercenaryStatUtils.getAuraGems(data)) {
                if (other.id.equals(gem.id)) {
                    return false;
                }
            }
            // and it has to fit in the remaining spirit. this picker is built client side off the local
            // player, and MercenaryManager.getMerc() is hard server-only - resolving through
            // ClientMercenary there instead keeps the client's estimate in step with the real
            // gear/aura-boosted spirit stat instead of falling back to the flat per-level value.
            int cost = (int) (gem.getAura().reservation * 100F);
            MercenaryEntity merc = owner.level().isClientSide ? ClientMercenary.get() : MercenaryManager.getMerc(owner);
            return MercenaryStatUtils.getRemainingSpirit(merc, data) >= cost;
        }
    };

    public abstract MyInventory inventoryOf(MercenaryData data);

    public abstract boolean mayPlace(Player owner, MercenaryData data, int index, ItemStack stack);

    /**
     * The strength/dexterity/intelligence gate on a piece of gear.
     * <p>
     * The level requirement above was the only one a mercenary was ever held to, so it could wear a
     * staff asking for two hundred Intelligence as long as it was high enough level. This is the
     * second half of {@code GearItemData.canPlayerWear} - the half {@code UnequipGear} enforces on
     * every player - applied to the mercenary rather than to its owner.
     * <p>
     * It needs the live entity, because the attributes come out of a stat calculation and a stored
     * loadout has none. Resolved the same way the AURA branch resolves its spirit check, and
     * skipped entirely when the mercenary is not out: refusing to let anyone gear up a dismissed
     * mercenary would be worse than letting {@code MercenaryManager.validateEquipment} hand the item
     * back the next time it spawns, which it already does for everything else that stops being legal.
     */
    private static boolean meetsAttributeReq(Player owner, GearItemData gear) {
        MercenaryEntity merc = owner.level().isClientSide ? ClientMercenary.get() : MercenaryManager.getMerc(owner);
        if (merc == null) {
            return true;
        }
        return gear.getRequirement().meetsReq(gear.getLevel(), Load.Unit(merc));
    }

    private static boolean fitsSlot(GearItemData gear, EquipmentSlot slot) {
        BaseGearType type = gear.GetBaseGearType();
        if (type == null || type.isJewelry()) {
            return false;
        }

        if (slot == EquipmentSlot.MAINHAND) {
            return type.isWeapon();
        }
        if (slot == EquipmentSlot.OFFHAND) {
            // shields, tomes and totems, plus a one hander when dual wielding
            return type.isOffhand() || (type.isWeapon() && DualWieldUtils.isDualWieldWeapon(gear));
        }
        // armor maps straight onto a vanilla slot
        return type.getVanillaSlotType() == slot;
    }

    /** hand a stack back to the player, dropping it at their feet if the inventory is full */
    public static void giveBack(Player p, ItemStack stack) {
        PlayerUtils.forceUnequipItem(stack, p);
    }

    /** everything that has to happen after any mercenary inventory changes */
    public static void afterChange(Player p) {
        MercenaryManager.refreshGear(p);
        Load.player(p).playerDataSync.setDirtyAndSync(p);
    }
}
