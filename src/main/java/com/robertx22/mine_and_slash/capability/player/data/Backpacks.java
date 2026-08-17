package com.robertx22.mine_and_slash.capability.player.data;

import com.robertx22.addons.orbs_of_crafting.currency.IItemAsCurrency;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.a_libraries.curios.CuriosSlots;
import com.robertx22.mine_and_slash.a_libraries.curios.MyCuriosUtils;
import com.robertx22.mine_and_slash.capability.player.container.BackpackMenu;
import com.robertx22.mine_and_slash.capability.player.helper.BackpackInventory;
import com.robertx22.mine_and_slash.capability.player.helper.BackpackLayouts;
import com.robertx22.mine_and_slash.database.data.game_balance_config.BackpackTabConfig;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.DataSaverCheckUtil;
import com.robertx22.mine_and_slash.vanilla_mc.items.gemrunes.GemItem;
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

    // the capability owns the nbt cache, so it hands us the invalidation hook to put on every
    // inventory. kept here because openBackpack can rebuild one and it has to stay wired up
    private transient Runnable onChanged = null;

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        for (BackpackInventory inv : map.values()) {
            inv.onChanged(onChanged);
        }
    }


    public enum BackpackType {
        GEARS("gear", Words.Gear) {
            @Override
            public boolean isValid(ItemStack stack) {
                return StackSaving.GEARS.has(stack) || StackSaving.JEWEL.has(stack) || StackSaving.STAT_SOULS.has(stack);
            }
        },
        MAPS("map", Words.Maps) {
            @Override
            public boolean isValid(ItemStack stack) {
                return StackSaving.MAP.has(stack) || DataSaverCheckUtil.checkForDataSaver("ancient_obelisks" + "_obelisk", stack) || DataSaverCheckUtil.checkForDataSaver("the_harvest" + "_map", stack);
            }
        },
        CURRENCY("currency", Words.Currency) {
            @Override
            public boolean isValid(ItemStack stack) {
                // socketables have their own tab now. excluding them here rather than relying on
                // SOCKETABLE coming first keeps tryAutoPickup correct whatever the enum order is
                if (SOCKETABLE.isValid(stack)) {
                    return false;
                }
                var cur = ExileCurrency.get(stack);
                if (cur.isPresent()) {
                    return true;
                }
                return stack.getItem() instanceof IItemAsCurrency || stack.getItem() instanceof RarityStoneItem;
            }
        },
        SOCKETABLE("socketable", Words.Socketables) {
            @Override
            public boolean isValid(ItemStack stack) {
                return stack.getItem() instanceof RuneItem || stack.getItem() instanceof GemItem;
            }
        },
        SKILL_GEMS("skill_gem", Words.SkillGem) {
            @Override
            public boolean isValid(ItemStack stack) {
                return StackSaving.SKILL_GEM.has(stack);
            }
        },
        PROFESSION("profession", Words.PROFESSIONS) {
            @Override
            public boolean isValid(ItemStack stack) {
                Item item = stack.getItem();
                return item instanceof IGoesToBackpack;
            }
        };

        public String id;
        public Words name;

        BackpackType(String id, Words name) {
            this.id = id;
            this.name = name;
        }

        // how many rows the window shows at once. tabs with dedicated slots hold more than this
        // and scroll, everything else is exactly this tall
        public int getRows() {
            return getConfig().rows;
        }

        public int getSize() {
            return getRows() * 9;
        }

        // the real container size, which for dedicated tabs comes from the registries
        public int getTotalSize() {
            if (BackpackLayouts.usesDedicatedSlots(this)) {
                int layout = BackpackLayouts.get(this).slots.size();
                if (layout > 0) {
                    return Math.max(layout, getSize());
                }
            }
            return getSize();
        }

        public int getTotalRows() {
            return getTotalSize() / 9;
        }

        public int getStackMultiplier() {
            return getConfig().stack_multiplier;
        }

        private static final BackpackTabConfig DEFAULT_CONFIG = new BackpackTabConfig(6, 1);

        public BackpackTabConfig getConfig() {
            // a datapack written before this tab existed has no entry for it, and this is reached
            // from the capability constructor where an NPE would be fatal
            return GameBalanceConfig.get().backpack_tabs.getOrDefault(this, DEFAULT_CONFIG);
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

        if (!hasBackpack(p)) {
            return false;
        }
        boolean result = false;
        for (BackpackType type : BackpackType.values()) {
            if (type.isValid(stack)) {
                var bag = getInv(type);

                if (bag.canAddItem(stack)) {
                    // a dedicated slot can fill up mid-stack, so only consume what actually went in
                    // and leave the rest on the ground rather than deleting it
                    ItemStack rest = bag.addItem(stack.copy());
                    int taken = stack.getCount() - rest.getCount();

                    if (taken > 0) {
                        stack.shrink(taken);
                        if (shouldPlaySound) SoundUtils.playSound(this.player, SoundEvents.ITEM_PICKUP);
                        result = true;
                        break;
                    }
                }
            }
        }

        return result;

    }

    // the backpack can only be used while the item is carried, either in the inventory or the curio slot
    public static boolean hasBackpack(Player p) {
        return p.getInventory().countItem(SlashItems.MASTER_BAG.get()) > 0 || hasCuriosBackpack(p);
    }

    private static boolean hasCuriosBackpack(Player p) {
        var backpackItem = MyCuriosUtils.get(CuriosSlots.MASTER_BAG.name, p, 0);
        return !backpackItem.isEmpty();
    }

    public boolean tryAutoPickup(Player p, ItemStack stack){
        return tryAutoPickup(p, stack, true);
    }
    // todo every time before you open backpack, it will replace locked slots with blocked slots that cant be clicked on and throw out/give items back

    /**
     * Puts the tabs in a state fit to be opened.
     * <p>
     * Two things can put them out of shape. Runes and gems used to live in the currency tab and
     * an existing save still has them there, and the contents are stored by slot index, so a
     * datapack adding or removing a currency shifts every reserved slot after it. Both are fixed
     * by re-homing everything rather than by trusting the saved indices.
     */
    public void prepareTabs() {
        // move anything sitting in a tab that no longer accepts it
        for (BackpackType type : BackpackType.values()) {
            BackpackInventory from = getInv(type);

            for (int i = 0; i < from.getContainerSize(); i++) {
                ItemStack stack = from.getItem(i);
                if (stack.isEmpty() || type.isValid(stack)) {
                    continue;
                }
                for (BackpackType other : BackpackType.values()) {
                    if (other == type || !other.isValid(stack)) {
                        continue;
                    }
                    BackpackInventory to = getInv(other);
                    if (to.canAddItem(stack)) {
                        // whatever didn't fit stays behind in the old tab
                        from.setItem(i, to.addItem(stack.copy()));
                    }
                    break;
                }
                // no room anywhere else, so it stays put rather than being destroyed
            }
        }

        for (BackpackType type : BackpackType.values()) {
            resizeIfNeeded(type);
            getInv(type).resortIntoLayout();
        }
    }

    // the container is sized when the capability is built. a datapack reload since then can have
    // changed how many reserved slots the tab needs
    private void resizeIfNeeded(BackpackType type) {
        BackpackInventory old = getInv(type);
        int wanted = type.getTotalSize();

        if (old.getContainerSize() == wanted) {
            return;
        }

        BackpackInventory fresh = new BackpackInventory(player, type);
        for (int i = 0; i < old.getContainerSize(); i++) {
            ItemStack stack = old.getItem(i);
            if (!stack.isEmpty()) {
                ItemStack rest = fresh.addItem(stack.copy());
                if (!rest.isEmpty() && !player.level().isClientSide) {
                    // only reachable if the tab got smaller. hand it back rather than delete it
                    player.getInventory().placeItemBackInInventory(rest);
                }
            }
        }
        if (onChanged != null) {
            fresh.onChanged(onChanged);
        }
        map.put(type, fresh);

        if (onChanged != null) {
            onChanged.run();
        }
    }

    public void openBackpack(BackpackType type, Player p) {
        if (!p.level().isClientSide) {
            if (!hasBackpack(p)) {
                return;
            }
            prepareTabs();
            BackpackInventory inv = getInv(type);
            p.openMenu(new SimpleMenuProvider((i, playerInventory, playerEntity) -> {
                return new BackpackMenu(type, i, playerEntity, playerInventory, inv);
            }, Component.literal("")));
        }
    }
}
