package com.robertx22.mine_and_slash.event_hooks.ontick;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// gives back the in between frames that a teleport deliberately throws away.
//
// every entity is drawn sliding from where it was to where it is, so a 20 tick a second game still
// looks continuous at 120 fps. a teleport switches that off - the packet that relocates you writes
// "where you were" and "where you are" to the same place, leaving nothing to slide along. that is
// exactly right for a blink, and exactly wrong for a skill that teleports you onto a moving
// projectile every single tick: instead of one honest snap you get eighty.
//
// the obvious repair - putting the real previous position back into those fields once per tick -
// loses a race it cannot win. ClientPacketListener.handleMovePlayer runs from Minecraft.runAllTasks,
// which is called once per rendered FRAME rather than once per tick (Minecraft.runTick), so the next
// teleport lands at some arbitrary frame in the middle of the tick and wipes the repair again for
// every frame after it.
//
// so nothing here writes to the player at all. instead it remembers where the player stood at the
// end of each tick and hands out, on request, the gap between the smooth path through those
// snapshots and whatever vanilla is about to draw. the two places that draw the player add it in.
// a packet landing mid frame moves what vanilla draws and moves this offset by the same amount the
// other way, so the picture does not budge and packet timing stops mattering.
//
// snapshots are taken from ClientTickEvent END - ForgeEventFactory.onPostClientTick, the last
// statement of Minecraft.tick - which is after the player has ticked and with no network flush in
// between, so a packet can never land partway through one.
public class SmoothTeleportRender {

    // above this a relocation is a real blink (dash, flicker strike, shadow step), a command, or a
    // world change, and it should still land instantly. below it, it is a skill dragging the player
    // along something - avalanche_vault steps about half a block per tick.
    // also the ceiling on how far the drawn player may ever sit from the real one
    private static final double MAX_SMOOTHED_TELEPORT = 3;

    private static Vec3 prevTickPos = null;
    private static Vec3 curTickPos = null;
    private static int lastPlayerId = -1;
    private static ResourceKey<Level> lastDimension = null;

    public static void onEndTick(Minecraft mc) {

        LocalPlayer player = mc.player;

        if (player == null) {
            reset();
            return;
        }

        if (player.getId() != lastPlayerId || player.level().dimension() != lastDimension) {
            // respawn or dimension change. the old snapshots belong to somewhere else entirely and
            // interpolating toward them would drag the camera across the world
            reset();
            lastPlayerId = player.getId();
            lastDimension = player.level().dimension();
        }

        prevTickPos = curTickPos;
        curTickPos = player.position();

        if (prevTickPos == null || prevTickPos.distanceToSqr(curTickPos) > MAX_SMOOTHED_TELEPORT * MAX_SMOOTHED_TELEPORT) {
            // a real blink happened inside that tick. collapsing the two snapshots means there is
            // nothing to interpolate across it, which is the instant landing a blink is supposed to have
            prevTickPos = curTickPos;
        }
    }

    /**
     * How far the smooth path is from where vanilla is about to draw the player this frame, in world
     * space. Add it to a camera position or translate a pose stack by it.
     * <p>
     * oldX/oldY/oldZ is the "where you were" the caller's own interpolation reads: xo/yo/zo for the
     * camera (Camera.setup), xOld/yOld/zOld for the model (LevelRenderer.renderEntity). They hold the
     * same value on every path that touches the local player, but Entity.absMoveTo writes only the
     * first pair, so it costs nothing to ask for the right one.
     * <p>
     * Pure - no state changes, safe to call as many times per frame as there are things to place.
     * Returns ZERO whenever it has nothing useful to say, which during ordinary movement is always:
     * setOldPosAndRot copies the position this class snapshotted a tick ago into xo, so the smooth
     * path and vanilla's own agree exactly and this cancels to nothing.
     */
    public static Vec3 offset(Player player, double oldX, double oldY, double oldZ, float partialTicks) {

        if (player != Minecraft.getInstance().player) {
            return Vec3.ZERO;
        }

        if (prevTickPos == null || curTickPos == null) {
            return Vec3.ZERO;
        }

        if (player.getId() != lastPlayerId || player.level().dimension() != lastDimension) {
            // a frame drawn before the tick that would have noticed the change
            return Vec3.ZERO;
        }

        Vec3 smooth = prevTickPos.lerp(curTickPos, partialTicks);
        Vec3 vanilla = new Vec3(oldX, oldY, oldZ).lerp(player.position(), partialTicks);

        Vec3 offset = smooth.subtract(vanilla);

        if (offset.lengthSqr() > MAX_SMOOTHED_TELEPORT * MAX_SMOOTHED_TELEPORT) {
            // a correction far too big to be a ride - a lag rubber band, or a blink whose packet
            // landed mid frame. let it snap rather than sliding the player across the world
            return Vec3.ZERO;
        }

        return offset;
    }

    private static void reset() {
        prevTickPos = null;
        curTickPos = null;
        lastPlayerId = -1;
        lastDimension = null;
    }
}
