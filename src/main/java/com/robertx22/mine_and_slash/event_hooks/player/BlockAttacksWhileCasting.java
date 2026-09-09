package com.robertx22.mine_and_slash.event_hooks.player;

import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.player.Player;

/**
 * A cast owns the player's hands: while a skill with a cast time (or a channel) is going, basic
 * attacks are refused rather than the cast being interrupted. This used to be the other way round
 * (the attack landed and cancelled the cast, recovery owed), which let a stray click throw a
 * four-swing skill away. Instant skills never sit in the casting state, so they are untouched.
 * <p>
 * Enforced by cancelling {@code AttackEntityEvent} on the server. Better Combat resolves its hits
 * through vanilla {@code Player.attack} per target, so the same cancel covers it; only its
 * client-side swing animation still plays.
 */
public class BlockAttacksWhileCasting {

    public static boolean isCasting(Player player) {
        if (player.level().isClientSide) {
            return false;
        }
        if (player.isDeadOrDying()) {
            return false;
        }
        return isCastingClient(player);
    }

    /**
     * The same test without the side guard, for the client mixin that refuses the swing before it
     * starts. The client ticks its own copy of the cast (OnClientTick -> onTimePass) and the
     * CAST_FINISH packet clears it, so this reads the state the cast bar is drawn from.
     */
    public static boolean isCastingClient(Player player) {
        try {
            var playerData = Load.player(player);
            if (playerData == null) {
                return false;
            }
            var data = playerData.spellCastingData;
            // same test the cast bar and the casting slow use
            return data.isCasting() && data.castTickLeft > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
