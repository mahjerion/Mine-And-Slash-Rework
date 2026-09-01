package com.robertx22.mine_and_slash.database.data.unique_items.collection;

import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Account binding for uniques reconstructed from the sticker book.
 * <p>
 * Minecraft has no way to stop an item being handed over - too many mods move stacks around - so the
 * binding is enforced on the stats instead: a bound item worn by anyone else simply contributes
 * nothing. It stays equipped and visible, it just does nothing, which is what makes it worthless to
 * trade without needing to block the trade itself.
 */
public class BoundItemUtils {

    /**
     * Reads the custom data straight off the saver rather than through ExileStack.
     * <p>
     * ExileStack.of copies the ItemStack, and this runs inside GearData.isUsableBy - once per equipped
     * piece per gear recalc, plus once per frame per bound item being hovered. There is nothing to
     * write here, so the copy would be pure waste.
     */
    @Nullable
    private static CustomItemData load(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !StackSaving.CUSTOM_DATA.has(stack)) {
            return null;
        }
        return StackSaving.CUSTOM_DATA.loadFrom(stack);
    }

    public static String getOwnerUuid(ItemStack stack) {
        return ownerUuidOf(load(stack));
    }

    private static String ownerUuidOf(@Nullable CustomItemData data) {
        if (data == null) {
            return "";
        }
        String owner = data.data.get(CustomItemData.KEYS.OWNER);
        return owner == null ? "" : owner;
    }

    public static String getOwnerName(ItemStack stack) {
        CustomItemData data = load(stack);
        if (data == null) {
            return "";
        }
        String name = data.data.get(CustomItemData.KEYS.OWNER_NAME);
        return name == null ? "" : name;
    }

    public static boolean isBound(ItemStack stack) {
        return !getOwnerUuid(stack).isEmpty();
    }

    /**
     * Stamps the binding onto a fresh stack. Returns the stamped stack - ExileStack copies, so the
     * caller must use the result rather than the stack it passed in.
     */
    public static ItemStack bindTo(ItemStack stack, Player player) {
        ExileStack ex = ExileStack.of(stack);
        ex.get(StackKeys.CUSTOM).edit(x -> {
            x.data.set(CustomItemData.KEYS.OWNER, player.getStringUUID());
            x.data.set(CustomItemData.KEYS.OWNER_NAME, player.getGameProfile().getName());
        });
        return ex.getStack();
    }

    /**
     * The one gate. Called from GearData.isUsableBy, which every stat path funnels through - armor
     * slots, weapons, curios and mercenaries alike - so this single check covers all of them.
     * <p>
     * An unbound item is always usable, so nothing that already exists in a world is affected.
     */
    public static boolean canUse(ItemStack stack, Entity wearer) {
        String owner = ownerUuidOf(load(stack));
        if (owner.isEmpty()) {
            return true;
        }
        if (wearer == null) {
            return false;
        }
        if (wearer instanceof Player p) {
            return p.getStringUUID().equals(owner);
        }
        // a mercenary is an extension of its owner, so it inherits their bindings
        if (wearer instanceof MercenaryEntity merc) {
            Player mercOwner = MercenaryManager.getOwnerOf(merc);
            return mercOwner != null && mercOwner.getStringUUID().equals(owner);
        }
        return false;
    }
}
