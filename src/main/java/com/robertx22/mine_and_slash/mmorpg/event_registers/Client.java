package com.robertx22.mine_and_slash.mmorpg.event_registers;

import com.robertx22.mine_and_slash.event_hooks.ontick.OnClientTick;
import com.robertx22.mine_and_slash.event_hooks.player.OnKeyPress;
import com.robertx22.mine_and_slash.mmorpg.ForgeEvents;
import com.robertx22.mine_and_slash.mmorpg.registers.client.KeybindsRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraftforge.client.event.ScreenEvent;
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

        });

        // clean up a bind left sitting on a bare modifier key before it can poison the next rebind
        ForgeEvents.registerForgeEvent(ScreenEvent.Closing.class, event -> {
            if (event.getScreen() instanceof KeyBindsScreen) {
                KeybindsRegister.sanitizeBinds();
            }
        });


    }
}
