package com.robertx22.mine_and_slash.mmorpg.event_registers;

import com.robertx22.mine_and_slash.event_hooks.ontick.OnClientTick;
import com.robertx22.mine_and_slash.event_hooks.ontick.SmoothTeleportRender;
import com.robertx22.mine_and_slash.event_hooks.player.OnKeyPress;
import com.robertx22.mine_and_slash.mmorpg.ForgeEvents;
import com.robertx22.mine_and_slash.mmorpg.registers.client.KeybindsRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;

public class Client {

    // options.txt is read after our keybinds are registered, so the migration has to wait for a tick
    private static boolean sanitizedBindsOnStartup = false;

    public static void register() {


        // todo
        // InputEvent.Key.class

        ForgeEvents.registerForgeEvent(TickEvent.ClientTickEvent.class, event -> {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (!sanitizedBindsOnStartup) {
                sanitizedBindsOnStartup = true;
                KeybindsRegister.sanitizeBinds();
            }
            OnClientTick.onEndTick(Minecraft.getInstance());
            OnKeyPress.onEndTick(Minecraft.getInstance());
            // last thing in the tick on purpose, see the class comment
            SmoothTeleportRender.onEndTick(Minecraft.getInstance());

        });

        // clean up a bind left sitting on a bare modifier key before it can poison the next rebind
        ForgeEvents.registerForgeEvent(ScreenEvent.Closing.class, event -> {
            if (event.getScreen() instanceof KeyBindsScreen) {
                KeybindsRegister.sanitizeBinds();
            }
        });

        // the two places the local player gets drawn, both nudged onto the smooth path that a per
        // tick teleport would otherwise skip. see SmoothTeleportRender - during ordinary movement
        // the offset is exactly zero and neither of these changes anything

        // fired right after Camera.setup and before the frustum and the level renderer read the
        // camera back, so this still lands in time. adding rather than replacing keeps the third
        // person zoom back that setup already applied
        ForgeEvents.registerForgeEvent(ViewportEvent.ComputeCameraAngles.class, event -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;

            if (player == null || mc.getCameraEntity() != player) {
                return; // spectating something else, that camera is not ours to move
            }

            Vec3 offset = SmoothTeleportRender.offset(player, player.xo, player.yo, player.zo, (float) event.getPartialTick());

            if (offset.lengthSqr() > 0) {
                event.getCamera().setPosition(event.getCamera().getPosition().add(offset));
            }
        });

        // the third person body. fired before the renderer pushes its own pose, with only the entity
        // dispatcher's translate on the stack, so this is world axis aligned. deliberately not undone
        // afterwards - the dispatcher pops it anyway, and leaving it on carries the shadow and the
        // fire overlay along with the body instead of detaching them from it
        ForgeEvents.registerForgeEvent(RenderPlayerEvent.Pre.class, event -> {
            Player player = event.getEntity();

            if (player != Minecraft.getInstance().player) {
                return; // everyone else is already interpolated by LivingEntity.lerpTo
            }

            Vec3 offset = SmoothTeleportRender.offset(player, player.xOld, player.yOld, player.zOld, event.getPartialTick());

            if (offset.lengthSqr() > 0) {
                event.getPoseStack().translate(offset.x, offset.y, offset.z);
            }
        });


    }
}
