package com.robertx22.mine_and_slash.event_hooks.player;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.vanilla_mc.packets.CancelItemUsePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.UseAnim;

/**
 * Clicking once per arrow is the actual damage cap on a ranged basic attack build, so this automates
 * the one step vanilla leaves to the player: letting go of the button at full draw.
 * <p>
 * A shot is two halves. The release is the vanilla key-up path, a single call -
 * gameMode.releaseUsingItem(player) - so that half is free. The re-draw vanilla would also give us
 * for free, via "if (keyUse.isDown() && rightClickDelay == 0 && !isUsingItem()) startUseItem()" in
 * handleKeybinds, but not usably: startUseItem right clicks the world before it reaches the item.
 * See reArm. So we drive the second half too, through gameMode.useItem.
 * <p>
 * Nothing about shooting, damage, ammo or enchantments is reimplemented. On the firing path the
 * server sees the same RELEASE_USE_ITEM and USE_ITEM packets a clicking player would send, and every
 * other mod runs exactly as it always did.
 * <p>
 * This runs on ClientTickEvent phase START on purpose. Minecraft.tick fires the START event before
 * handleKeybinds, so releasing and re-arming here both land before vanilla gets a look at the key
 * state, and a shot costs no extra ticks.
 * <p>
 * The same START ordering is what lets us pre-empt the other half of handleKeybinds: a key-up while
 * the bow is only part drawn, which vanilla reads as "shoot now" and turns into a weak arrow. Since
 * auto fire started that draw rather than the player, letting go means "stop shooting", so the draw
 * is cancelled instead - locally plus a CancelItemUsePacket so the server drops it as well.
 * <p>
 * Client only: ItemProperties is @OnlyIn(Dist.CLIENT). Only ever referenced from
 * {@link com.robertx22.mine_and_slash.mmorpg.event_registers.Client}, which is client side.
 */
public class AutoFireBows {

    // an instant draw modded bow would otherwise spray arrows as fast as the client ticks. this is
    // the same floor the INSTANT_ARROWS (Quickdraw) path already enforces in CommonEvents. a normal
    // 20 tick bow never comes near it
    private static final int MIN_TICKS_BETWEEN_SHOTS = 3;

    // the client and the server each count the draw in their own ticks, and the use and release
    // packets can slip a tick independently, so the server can end up a tick or two short of the
    // charge the client sees. a human never notices because they hold the button well past full
    // draw; releasing on the exact tick the string maxes out would remove that slack entirely.
    // being short is silent and expensive: a bow loses damage through ARROW_DRAW_AMOUNT_MULTI, and a
    // crossbow fails the "power >= 1" check in CrossbowItem.releaseUsing outright, never charges,
    // and loops forever without firing. so hold the full draw a few extra ticks before letting go
    private static final int EXTRA_DRAW_TICKS = 3;

    // the model property every bow needs in order to render its draw, and therefore the most
    // compatible "how far back is the string" answer available for a modded bow
    private static final ResourceLocation PULL = new ResourceLocation("pull");

    // countdowns rather than tick stamps: the client builds a fresh LocalPlayer with tickCount 0 on
    // respawn and on dimension change, which would leave a stamped comparison stuck forever
    private static int shotCooldown = 0;
    private static int ticksAtFullDraw = 0;

    public static void onStartTick(Minecraft mc) {

        try {
            if (shotCooldown > 0) {
                shotCooldown--;
            }

            LocalPlayer player = mc.player;

            if (player == null || mc.level == null || mc.gameMode == null) {
                ticksAtFullDraw = 0;
                return;
            }
            // vanilla gates handleKeybinds on both of these, so with either one up it is not going
            // to release or re-draw anything and there is nothing here to pre-empt. the draw keeps
            // charging behind the GUI, so by the time it closes the bow is essentially always full
            // and vanilla's release is the shot the player wants
            if (mc.screen != null || mc.getOverlay() != null || mc.isPaused()) {
                ticksAtFullDraw = 0;
                return;
            }
            if (player.isDeadOrDying() || !player.isUsingItem()) {
                ticksAtFullDraw = 0;
                return;
            }

            ItemStack stack = player.getUseItem(); // the pull property compares this by identity

            if (!isBowLike(stack)) {
                ticksAtFullDraw = 0;
                return;
            }

            // Load.player never returns null, it falls back to a blank PlayerData whose empty config
            // map reads as "off". So in the moment before the first sync lands this is simply
            // inactive rather than throwing
            if (!Load.player(player).config.isConfigEnabled(PlayerConfigData.Config.AUTO_FIRE_BOWS)) {
                ticksAtFullDraw = 0;
                return;
            }

            Draw draw = drawStateOf(mc, player, stack);

            if (!mc.options.keyUse.isDown()) {
                ticksAtFullDraw = 0;

                // vanilla is about to loose this bow in handleKeybinds later this very tick, because
                // it reads a key-up as "shoot now" at whatever charge the string happens to be at.
                // but auto fire started this draw, not the player, so letting go means "stop
                // shooting" instead - cancel it rather than spitting out a weak arrow
                if (draw == Draw.PARTIAL) {
                    player.stopUsingItem(); // clears startedUsingItem, so vanilla skips its release

                    // vanilla drains these in its using-item branch and acts on them in the other
                    // one. clearing startedUsingItem just flipped handleKeybinds onto the acting
                    // branch for the rest of this tick, so a click queued in this same tick would
                    // suddenly take effect - opening the chest under the crosshair instead of being
                    // swallowed. drain them exactly as the branch we diverted from would have
                    while (mc.options.keyUse.consumeClick()) {
                    }
                    while (mc.options.keyAttack.consumeClick()) {
                    }
                    while (mc.options.keyPickItem.consumeClick()) {
                    }

                    Packets.sendToServer(new CancelItemUsePacket()); // and the server drops it too
                }
                return; // already at full draw - let vanilla fire it, that shot is worth keeping
            }

            if (draw != Draw.FULL) {
                ticksAtFullDraw = 0;
                return;
            }

            if (ticksAtFullDraw++ < EXTRA_DRAW_TICKS) {
                return; // hold it a moment longer so the server also sees a full draw
            }
            if (shotCooldown > 0) {
                return;
            }

            shotCooldown = MIN_TICKS_BETWEEN_SHOTS;
            ticksAtFullDraw = 0;

            InteractionHand hand = player.getUsedItemHand(); // capture before the release clears it

            mc.gameMode.releaseUsingItem(player); // the exact call vanilla makes when you let go

            reArm(mc, player, hand);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Start the next draw ourselves rather than leaving it to vanilla.
     * <p>
     * handleKeybinds would re-arm us for free - that is the whole trick this class rests on - but it
     * does it through startUseItem, which tries a block or entity interaction FIRST and only reaches
     * the item if nothing consumed the click. Holding the button through a release therefore hands
     * vanilla a fresh right click on the world once per shot, so shooting past a chest opens it. That
     * cannot happen in vanilla because loosing an arrow means letting go of the button, which is
     * exactly the state startUseItem is gated on.
     * <p>
     * The player clicked once to start shooting. The automation should repeat "use the bow", not
     * re-issue "click on whatever is under the crosshair" - and gameMode.useItem is precisely the
     * second half of startUseItem, minus the interaction. Re-arming also leaves isUsingItem true,
     * which is what stops vanilla running startUseItem later in this same tick.
     */
    private static void reArm(Minecraft mc, LocalPlayer player, InteractionHand hand) {

        // twice at most. a bow needs one call - use() starts the draw. a crossbow needs two, because
        // the first only looses the loaded bolt and returns without starting anything, so stopping
        // there would leave isUsingItem false and hand the tick straight back to vanilla
        for (int i = 0; i < 2 && !player.isUsingItem(); i++) {
            if (!mc.gameMode.useItem(player, hand).consumesAction()) {
                return; // out of ammo or something refused it. let vanilla have the tick, as before
            }
            // vanilla does this for a consumed use, so keep the held item animation identical
            mc.gameRenderer.itemInHandRenderer.itemUsed(hand);
        }
    }

    // deliberately narrow. food, shields, spyglasses, goat horns and tridents all hold a use state
    // too, and none of them should ever be fired for the player
    private static boolean isBowLike(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof ProjectileWeaponItem) {
            return true;
        }
        UseAnim anim = stack.getUseAnimation();

        // a modded bow that skips ProjectileWeaponItem still has to render its draw, so requiring the
        // pull property keeps this from grabbing something that merely borrowed the bow animation
        return (anim == UseAnim.BOW || anim == UseAnim.CROSSBOW) && getPullProperty(stack) != null;
    }

    /**
     * UNKNOWN has to be its own answer rather than collapsing into PARTIAL. Firing and cancelling ask
     * opposite questions, and an item whose draw we cannot read must get "no" to both - otherwise a
     * modded weapon we can't measure would have every one of its manual releases cancelled and could
     * never be fired at all while the feature is on.
     */
    private enum Draw {
        FULL,
        PARTIAL,
        UNKNOWN
    }

    private static Draw drawStateOf(Minecraft mc, LocalPlayer player, ItemStack stack) {

        ItemPropertyFunction pull = getPullProperty(stack);

        if (pull != null) {
            float value = Float.NaN;

            try {
                // vanilla registers this for the bow as charge/20 and for the crossbow as
                // charge/chargeDuration, and modded bows follow suit
                value = pull.call(stack, mc.level, player, 0);
            } catch (Exception e) {
                // a third party property function is not worth a crash, fall through to the rules below
            }

            // Quick Charge V drives getChargeDuration to 0, so the vanilla crossbow property divides
            // by zero: NaN on the first tick, then +Infinity. NaN would silently never fire, so let
            // that fall through to the class rules instead, and treat +Infinity as drawn
            if (value == Float.POSITIVE_INFINITY) {
                return Draw.FULL;
            }
            if (Float.isFinite(value)) {
                return value >= 1.0F ? Draw.FULL : Draw.PARTIAL;
            }
        }

        int remaining = player.getUseItemRemainingTicks();
        int duration = stack.getUseDuration();

        // crossbow likes only. useOnRelease items never self terminate, so the draw really is done
        // once the duration has elapsed. items that DO self complete keep counting past zero on the
        // client (completeUsingItem is server side only), so without this gate we could fire a
        // release at nothing
        if (stack.useOnRelease()) {
            return duration - remaining >= duration ? Draw.FULL : Draw.PARTIAL;
        }

        if (stack.getItem() instanceof BowItem) {
            // where BowItem.getPowerForTime reaches 1.0
            return duration - remaining >= 20 ? Draw.FULL : Draw.PARTIAL;
        }

        // no draw model we can read. this fires nothing AND cancels nothing, so the item behaves
        // exactly like vanilla: we never force a partial draw shot, which would silently lose damage
        // to ARROW_DRAW_AMOUNT_MULTI, and we never abort a release the player made deliberately
        return Draw.UNKNOWN;
    }

    private static ItemPropertyFunction getPullProperty(ItemStack stack) {
        try {
            return ItemProperties.getProperty(stack.getItem(), PULL);
        } catch (Exception e) {
            return null;
        }
    }
}
