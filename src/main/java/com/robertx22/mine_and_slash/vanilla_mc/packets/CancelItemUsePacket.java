package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.UseAnim;

/**
 * Drop the item the sender is currently using without releasing it.
 * <p>
 * Vanilla has no packet for this. RELEASE_USE_ITEM maps straight onto releaseUsingItem, which for a
 * bow means "shoot it now at whatever charge you happen to be at", and a hotbar swap only cancels
 * when the slot actually changes. So an auto fired bow that the player lets go of half drawn has no
 * way to tell the server "I did not want that arrow" - see AutoFireBows.
 * <p>
 * stopUsingItem empties useItem, and releaseUsingItem is guarded on that being non empty, so after
 * this the shot cannot happen on either side.
 */
public class CancelItemUsePacket extends MyPacket<CancelItemUsePacket> {

    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("cancel_item_use");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {

    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {

        Player p = ctx.getPlayer();

        if (p == null || !p.isUsingItem()) {
            return;
        }

        // inert for anyone who never turned the feature on, which is the only thing that sends this
        if (!Load.player(p).config.isConfigEnabled(PlayerConfigData.Config.AUTO_FIRE_BOWS)) {
            return;
        }

        ItemStack stack = p.getUseItem();
        UseAnim anim = stack.getUseAnimation();

        // deliberately narrow. this is only ever sent for a bow draw auto fire started on the
        // player's behalf, and keeping the check here stops it being a general "abort whatever I am
        // doing" lever - eating, blocking and the like stay out of reach.
        //
        // this duplicates AutoFireBows.isBowLike on purpose, do not "clean it up" by calling it:
        // isBowLike reads ItemProperties, which is @OnlyIn(Dist.CLIENT), so touching AutoFireBows
        // from here would NoClassDefFoundError on a dedicated server. the copy also has to stay at
        // least as permissive as the client's or legitimate cancels get silently dropped - the
        // client's extra "has a pull property" requirement only ever narrows
        if (stack.getItem() instanceof ProjectileWeaponItem || anim == UseAnim.BOW || anim == UseAnim.CROSSBOW) {
            p.stopUsingItem();
        }
    }

    @Override
    public MyPacket<CancelItemUsePacket> newInstance() {
        return new CancelItemUsePacket();
    }
}
