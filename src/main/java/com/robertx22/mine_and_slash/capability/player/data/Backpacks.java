package com.robertx22.mine_and_slash.capability.player.data;

import com.robertx22.addons.orbs_of_crafting.currency.IItemAsCurrency;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.a_libraries.curios.CuriosSlots;
import com.robertx22.mine_and_slash.a_libraries.curios.MyCuriosUtils;
import com.robertx22.mine_and_slash.capability.player.container.BackpackMenu;
import com.robertx22.mine_and_slash.capability.player.helper.BackpackInventory;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.DataSaverCheckUtil;
import com.robertx22.mine_and_slash.vanilla_mc.items.gemrunes.RuneItem;
import com.robertx22.mine_and_slash.vanilla_mc.items.misc.RarityStoneItem;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;

public class Backpacks {

    public Backpacks(Player player) {
        this.player = player;

        for (BackpackType type : BackpackType.values()) {
            map.put(type, new BackpackInventory(player, type));
        }
    }


    public enum BackpackType {
        GEARS("gear", Words.Gear, 6, 1) {
            @Override
            public boolean isValid(ItemStack stack) {
                return StackSaving.GEARS.has(stack) || StackSaving.JEWEL.has(stack) || StackSaving.STAT_SOULS.has(stack);
            }
        },
        MAPS("map", Words.Maps, 6, 1) {
            @Override
            public boolean isValid(ItemStack stack) {
                return StackSaving.MAP.has(stack) || DataSaverCheckUtil.checkForDataSaver("ancient_obelisks" + "_obelisk", stack) || DataSaverCheckUtil.checkForDataSaver("the_harvest" + "_map", stack);
            }
        },
        CURRENCY("currency", Words.Currency, 9, 64) {
            @Override
            public boolean isValid(ItemStack stack) {
                var cur = ExileCurrency.get(stack);
                if (cur.isPresent()) {
                    return true;
                }
                return stack.getItem() instanceof IItemAsCurrency || stack.getItem() instanceof RuneItem || stack.getItem() instanceof RarityStoneItem;
            }
        },
        SKILL_GEMS("skill_gem", Words.SkillGem, 6, 1) {
            @Override
            public boolean isValid(ItemStack stack) {
                return StackSaving.SKILL_GEM.has(stack);
            }
        },
        PROFESSION("profession", Words.PROFESSIONS, 6, 64) {
            @Override
            public boolean isValid(ItemStack stack) {
                Item item = stack.getItem();
                return item instanceof IGoesToBackpack;
            }
        };

        public String id;
        public Words name;
        public int rows;
        public int stackMultiplier;

        BackpackType(String id, Words name, int rows, int stackMultiplier) {
            this.id = id;
            this.name = name;
            this.rows = rows;
            this.stackMultiplier = stackMultiplier;
        }

        public int getSize() {
            return 9 * rows;
        }

        public ResourceLocation getIcon() {
            return SlashRef.guiId("backpack/" + id);
        }

        public abstract boolean isValid(ItemStack stack);
    }

    Player player;

    private HashMap<BackpackType, BackpackInventory> map = new HashMap<>();


    public BackpackInventory getInv(BackpackType type) {
        return map.get(type);
    }


    public boolean tryAutoPickup(Player p, ItemStack stack, boolean shouldPlaySound) {

        if (p.getInventory().countItem(SlashItems.MASTER_BAG.get()) < 1 && !hasCuriosBackpack(p)) {
            return false;
        }
        boolean result = false;
        for (BackpackType type : BackpackType.values()) {
            if (type.isValid(stack)) {
                var bag = getInv(type);

                if (bag.canAddItem(stack)) {
                    bag.addItem(stack.copy());
                    stack.shrink(stack.getCount() + 10); // just in case
                    if (shouldPlaySound) SoundUtils.playSound(this.player, SoundEvents.ITEM_PICKUP);
                    result = true;
                    break;
                }
            }
        }

        return result;

    }

    private boolean hasCuriosBackpack(Player p) {
        var backpackItem = MyCuriosUtils.get(CuriosSlots.MASTER_BAG.name, p, 0);
        return !backpackItem.isEmpty();
    }

    public boolean tryAutoPickup(Player p, ItemStack stack){
        return tryAutoPickup(p, stack, true);
    }
    // todo every time before you open backpack, it will replace locked slots with blocked slots that cant be clicked on and throw out/give items back

    public void openBackpack(BackpackType type, Player p) {
        if (!p.level().isClientSide) {
            BackpackInventory inv = getInv(type);
            p.openMenu(new SimpleMenuProvider((i, playerInventory, playerEntity) -> {
                return new BackpackMenu(type, i, playerEntity, playerInventory, inv);
            }, Component.literal("")));
        }
    }
}
