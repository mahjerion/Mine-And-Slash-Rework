package com.robertx22.mine_and_slash.capability.player.container;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.SetBackpackContentPacket;
import com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.SetBackpackSlotPacket;

import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;

public class BackpackSynchronizer implements ContainerSynchronizer {
    ServerPlayer player;

    BackpackSynchronizer(ServerPlayer player) {
        this.player = player;
    }

    public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> items, ItemStack carriedItem, int[] data) {
        Packets.sendToClient(player, new SetBackpackContentPacket(menu.containerId, menu.incrementStateId(), items, carriedItem));

        for(int index = 0; index < data.length; ++index) {
            this.broadcastDataValue(menu, index, data[index]);
        }

    }

    public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack item) {
        Packets.sendToClient(player, new SetBackpackSlotPacket(menu.containerId, menu.incrementStateId(), slot, item));
    }

    public void sendCarriedChange(AbstractContainerMenu menu, ItemStack item) {
        player.connection.send(new ClientboundContainerSetSlotPacket(-1, menu.incrementStateId(), -1, item));
    }

    public void sendDataChange(AbstractContainerMenu menu, int index, int data) {
        this.broadcastDataValue(menu, index, data);
    }

    private void broadcastDataValue(AbstractContainerMenu menu, int index, int data) {
        player.connection.send(new ClientboundContainerSetDataPacket(menu.containerId, index, data));
    }
}
