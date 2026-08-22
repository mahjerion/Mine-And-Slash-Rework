package com.robertx22.mine_and_slash.vanilla_mc.packets.spells;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class TellServerToCastSpellPacket extends MyPacket<TellServerToCastSpellPacket> {

    // every hotbar slot whose key is down right now, one bit each. the server queues these in slot
    // order, so a key shared by several skills plays them one after another instead of at once
    int heldMask;

    public TellServerToCastSpellPacket(int heldMask) {
        this.heldMask = heldMask;
    }

    public TellServerToCastSpellPacket() {
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "tell_server_castspell");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        this.heldMask = tag.readInt();
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeInt(heldMask);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Load.player(ctx.getPlayer()).spellCastingData.onSpellInputPressed(heldMask);

    }

    @Override
    public MyPacket<TellServerToCastSpellPacket> newInstance() {
        return new TellServerToCastSpellPacket();
    }
}
