package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.addons.map_device.MapDeviceClientState;
import com.robertx22.mine_and_slash.gui.inv_gui.SalvageMapFilterScreen;
import com.robertx22.mine_and_slash.gui.inv_gui.SalvageSubFilterScreen;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleAutoSalvageRarity;
import com.robertx22.mine_and_slash.gui.screens.map_device.MapDeviceScreen;
import com.robertx22.mine_and_slash.gui.screens.mercenary.MercenaryScreen;
import com.robertx22.mine_and_slash.gui.screens.stat_gui.StatScreen;
import com.robertx22.mine_and_slash.prophecy.gui.ProphecyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ClientOnly {

    public static int ticksSinceChatWasOpened = 0;

    public static void totemAnimWithItem(ItemStack stack) {
        Minecraft.getInstance().player.playSound(SoundEvents.TOTEM_USE, 1, 1);
        Minecraft.getInstance().gameRenderer.displayItemActivation(stack);
    }

    public static void openProphecy() {
        Minecraft.getInstance().setScreen(new ProphecyScreen());
    }


    public static Entity getEntityByUUID(Level world, UUID id) {

        if (world instanceof ClientLevel) {
            for (Entity entity : ((ClientLevel) world).entitiesForRendering()) {
                if (entity.getUUID()
                        .equals(id)) {

                    return entity;
                }
            }
        }
        return null;

    }

    public static Player getPlayerById(UUID id) {

        try {
            return Minecraft.getInstance().level.getPlayerByUUID(id);
        } catch (Exception e) {

        }
        return null;
    }

    public static Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    public static void setScreen(Screen s) {
        Minecraft.getInstance().setScreen(s);
    }

    public static void openEntityStatScreen(LivingEntity entity) {
        setScreen(new StatScreen(entity));
    }

    public static void closeScreen() {
        Minecraft.getInstance().setScreen(null);
    }

    // The inv gui actions below are constructed on the server too (GuiAction.regenActionMap runs there to
    // look up packets), so their classes must not mention a client Screen subclass anywhere in their
    // bytecode: the verifier loads both sides of a Screen assignment and the dist cleaner refuses the
    // client class on a dedicated server, which killed the whole action registry. Building the screens
    // in here keeps the action classes free of client types, like openEntityStatScreen above.

    public static void openSalvageSubFilter(ToggleAutoSalvageRarity.SalvageType type, int page) {
        setScreen(new SalvageSubFilterScreen(type, page));
    }

    public static void openSalvageMapFilter(int page) {
        setScreen(new SalvageMapFilterScreen(page));
    }

    public static void openMercenaryScreen() {
        setScreen(new MercenaryScreen());
    }

    /** back to the map device the player was using, or just close if there is no snapshot to show */
    public static void openMapDeviceScreenOrClose() {
        if (MapDeviceClientState.last != null) {
            setScreen(new MapDeviceScreen(MapDeviceClientState.last));
        } else {
            closeScreen();
        }
    }


    public static void pressUseKey() {
        Minecraft.getInstance().options.keyUse.setDown(true);
    }


    public static void stopUseKey() {
        Minecraft.getInstance().options.keyUse.setDown(false);
    }

    public static void printInChat(MutableComponent text) {
        Minecraft.getInstance().player.sendSystemMessage(text);
    }

    public static AbstractContainerMenu getContainerMenu() {
        return Minecraft.getInstance().player.containerMenu;
    }

}
