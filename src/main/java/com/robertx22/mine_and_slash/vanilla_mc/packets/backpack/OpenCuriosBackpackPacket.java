package com.robertx22.mine_and_slash.vanilla_mc.packets.backpack;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.a_libraries.curios.CuriosSlots;
import com.robertx22.mine_and_slash.a_libraries.curios.MyCuriosUtils;
import com.robertx22.mine_and_slash.capability.player.BackpackItem;
import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class OpenCuriosBackpackPacket extends MyPacket<OpenCuriosBackpackPacket> {

    public OpenCuriosBackpackPacket() {}


    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("open_curio_backpack");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {}

    @Override
    public void saveToData(FriendlyByteBuf buf) {}

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Player player = ctx.getPlayer();
        var backpackItem = MyCuriosUtils.get(CuriosSlots.MASTER_BAG.name, player, 0);
        if (backpackItem.isEmpty()) {
            return;
        }

        if (backpackItem.getItem() instanceof BackpackItem i) {
            var rows = i.getSlots() / 9;
            Load.backpacks(ctx.getPlayer()).getBackpacks().openBackpack(Backpacks.BackpackType.GEARS, ctx.getPlayer(), rows);
        }
    }

    @Override
    public MyPacket<OpenCuriosBackpackPacket> newInstance() {
        return new OpenCuriosBackpackPacket();
    }
}
