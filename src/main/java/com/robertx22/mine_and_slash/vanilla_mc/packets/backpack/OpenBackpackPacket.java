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
import org.jetbrains.annotations.Nullable;

public class OpenBackpackPacket extends MyPacket<OpenBackpackPacket> {

    public Backpacks.BackpackType type;

    public OpenBackpackPacket(Backpacks.BackpackType type) {
        this.type = type;
    }


    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("open_backpack");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        this.type = buf.readEnum(Backpacks.BackpackType.class);
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {

        buf.writeEnum(type);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Player player = ctx.getPlayer();
        BackpackItem backpack = getBackpackItem(player);

        if (backpack != null) {
            int rows = backpack.getSlots() / 9;
            Load.backpacks(player).getBackpacks().openBackpack(type, player, rows);
        }
    }

    private static @Nullable BackpackItem getBackpackItem(Player player) {
        if (player.getMainHandItem().getItem() instanceof BackpackItem mainHand) {
            return mainHand;
        }

        var curioBackpack = MyCuriosUtils.get(CuriosSlots.MASTER_BAG.name, player, 0);
        if (!curioBackpack.isEmpty() && curioBackpack.getItem() instanceof BackpackItem curioBackpackItem) {
            return curioBackpackItem;
        }

        return null;
    }

    @Override
    public MyPacket<OpenBackpackPacket> newInstance() {
        return new OpenBackpackPacket(Backpacks.BackpackType.GEARS);
    }
}
