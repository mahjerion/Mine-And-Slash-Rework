package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.capability.player.container.JewelsMenu;
import com.robertx22.mine_and_slash.capability.player.helper.JewelInvHelper;
import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashContainers;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

public class OpenJewelsPacket extends MyPacket<OpenJewelsPacket> {


    public OpenJewelsPacket() {

    }


    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("openjewels");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {


    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        var jewels = Load.player(ctx.getPlayer()).getJewels();
        Player p = ctx.getPlayer();
        p.openMenu(new SimpleMenuProvider((i, playerInventory, playerEntity) -> {
            MyInventory jewelInventory = jewels.jewelInventory;
            return new JewelsMenu(SlashContainers.JEWEL.get(), i, playerInventory, jewelInventory, playerEntity);
        }, Component.literal("")));

    }


    @Override
    public MyPacket<OpenJewelsPacket> newInstance() {
        return new OpenJewelsPacket();
    }
}
