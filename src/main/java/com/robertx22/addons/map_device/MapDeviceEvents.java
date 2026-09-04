package com.robertx22.addons.map_device;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.uncommon.ExplainedResultUtil;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import net.minecraft.world.entity.player.Player;

/**
 * The dungeon map device, the harvest block and the obelisk block all fire OPEN_MAP_DEVICE when
 * right-clicked; this is where the main mod answers.
 * <p>
 * A plain click sends that player the device's snapshot, which opens the shared device screen on their
 * client. A crouch click skips the screen: a map held in the main hand is slotted first (replacing one
 * already there), then the run starts or the live one is joined - through the very same server path as
 * the screen's button, so every gate (level, cooldown, resists, tickets, generation wait, grace) applies
 * unchanged. With nothing to enter, the player is told no map is loaded.
 */
public class MapDeviceEvents {

    public static void init() {
        ExileEvents.OPEN_MAP_DEVICE.register(new EventConsumer<ExileEvents.OpenMapDeviceEvent>() {
            @Override
            public void accept(ExileEvents.OpenMapDeviceEvent event) {
                Player p = event.player;
                var device = MapDeviceServer.resolve(p, event.pos);
                if (device == null) {
                    return;
                }

                if (!p.isCrouching()) {
                    MapDeviceServer.sync(p, device, event.pos);
                    return;
                }

                MapDeviceServer.insertHeldMap(p, device);

                var result = MapDeviceServer.tryStartOrJoin(p, device);
                if (result == MapDeviceServer.StartResult.NOTHING_TO_DO) {
                    p.sendSystemMessage(ExplainedResultUtil.createErrorAndReason(Chats.MAP_DEVICE_USE_ERROR, Chats.MAP_DEVICE_NO_MAP_LOADED));
                }
                // REFUSED: the gate that refused already said why
            }
        });
    }
}
