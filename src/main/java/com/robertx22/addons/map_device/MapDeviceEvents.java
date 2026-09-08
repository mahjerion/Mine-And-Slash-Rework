package com.robertx22.addons.map_device;

import com.robertx22.library_of_exile.dimension.device.IMapDeviceBlockEntity;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.mine_and_slash.mmorpg.ForgeEvents;
import com.robertx22.mine_and_slash.uncommon.ExplainedResultUtil;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * The dungeon map device, the harvest block and the obelisk block all fire OPEN_MAP_DEVICE when
 * right-clicked; this is where the main mod answers.
 * <p>
 * A plain click sends that player the device's snapshot, which opens the shared device screen on their
 * client. A crouch click skips the screen: a map held in the main hand is slotted first (replacing one
 * already there), then the run starts or the live one is joined - through the very same server path as
 * the screen's button, so every gate (level, cooldown, resists, tickets, generation wait, grace) applies
 * unchanged. With nothing to enter, the player is told no map is loaded.
 * <p>
 * Vanilla never calls a block's use() for a sneaking player holding anything in either hand, which would
 * make the crouch shortcut need empty hands - the opposite of "crouch with the map to enter". The
 * right-click hook below forces the block call for devices, on both sides, so the shortcut sees the
 * click no matter what is held; the blocks return SUCCESS, so the held item itself is never used.
 */
public class MapDeviceEvents {

    public static void init() {
        ForgeEvents.registerForgeEvent(PlayerInteractEvent.RightClickBlock.class, event -> {
            Player p = event.getEntity();
            if (p == null || !p.isSecondaryUseActive()) {
                return;
            }
            if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof IMapDeviceBlockEntity)) {
                return;
            }
            event.setUseBlock(Event.Result.ALLOW);
            event.setUseItem(Event.Result.DENY);
        });

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
