package com.robertx22.mine_and_slash.event_hooks.ontick;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// gives back the in between frames that a teleport deliberately throws away.
//
// every entity is drawn sliding from where it was to where it is, so a 20 tick a second game
// still looks continuous at 120 fps. Entity.absMoveTo, which is how the server relocates a
// player, overwrites "where it was" with "where it is" - there is nothing left to slide along.
// that is exactly right for a blink, and exactly wrong for a skill that teleports the player
// onto a moving projectile every single tick: instead of one honest snap you get eighty, the
// camera sitting still for 50ms then jumping half a block, over and over.
//
// this runs at the very end of the client tick (ForgeEventFactory.onPostClientTick, the last
// statement of Minecraft.tick), by which point the teleport packet has landed and the player
// has ticked. putting the genuine previous position back into the anchor fields leaves vanilla
// to draw the movement it was about to skip.
//
// it cannot leak into gameplay: ClientLevel.tickNonPassenger calls setOldPosAndRot() at the top
// of every player tick, so whatever is written here is overwritten before physics run again.
// the one visible consequence is that Minecraft.pick reads the same interpolated position, so
// the crosshair trails the camera by up to a tick of travel while a teleport is being smoothed.
public class SmoothTeleportRender {

    // above this a teleport is a real blink (dash, flicker strike, shadow step), a command, or a
    // world change, and it should still land instantly. below it, it is a skill dragging the
    // player along something - avalanche_vault steps about half a block per tick
    private static final double MAX_SMOOTHED_TELEPORT = 3;

    // ordinary movement leaves the anchor exactly where we last saw the player, so any gap at all
    // means something relocated them. compared squared, loosely enough to ignore double rounding
    private static final double EPSILON = 1.0E-8;

    private static Vec3 lastTickEndPos = null;
    private static int lastPlayerId = -1;
    private static ResourceKey<Level> lastDimension = null;

    public static void onEndTick(Minecraft mc) {

        LocalPlayer player = mc.player;

        if (player == null) {
            reset();
            return;
        }

        if (player.getId() != lastPlayerId || player.level().dimension() != lastDimension) {
            // respawn or dimension change. the old anchor belongs to somewhere else entirely and
            // interpolating toward it would drag the camera across the world
            reset();
        }

        if (lastTickEndPos != null) {

            double jumped = new Vec3(player.xo, player.yo, player.zo).distanceToSqr(lastTickEndPos);

            if (jumped > EPSILON && jumped <= MAX_SMOOTHED_TELEPORT * MAX_SMOOTHED_TELEPORT) {
                // xo feeds the camera (Camera.setup), xOld feeds the player model
                // (EntityRenderDispatcher). both, or first and third person disagree
                player.xo = player.xOld = lastTickEndPos.x;
                player.yo = player.yOld = lastTickEndPos.y;
                player.zo = player.zOld = lastTickEndPos.z;
            }
        }

        lastTickEndPos = player.position();
        lastPlayerId = player.getId();
        lastDimension = player.level().dimension();
    }

    private static void reset() {
        lastTickEndPos = null;
        lastPlayerId = -1;
        lastDimension = null;
    }
}
