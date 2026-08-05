package com.robertx22.mine_and_slash.prophecy;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

public class AcceptProphecyAffixPacket extends MyPacket<AcceptProphecyPacket> {

    String id;

    public AcceptProphecyAffixPacket(String id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return SlashRef.id("accept_prophecy_affix");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {

        id = buf.readUtf();
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {

        buf.writeUtf(id);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {

        var p = ctx.getPlayer();
        var data = Load.player(p).prophecy;

        if (data.affixOffers.contains(this.id)) {
            if (data.numMobAffixesCanAdd > 0 && data.affixesTaken.size() < 9) {
                data.affixesTaken.removeIf(x -> x == null || x.isEmpty() || !ExileDB.MapAffixes().isRegistered(x));

                data.affixesTaken.add(id);

                data.numMobAffixesCanAdd--;

                // Twin Curse - roll a fresh set for the next pick and put the screen back up.
                // without this the offers stay empty and the leftover budget blocks every altar in
                // the map for good. getProphecyCardsScreen() needs exactly 3, so a dry pool counts
                // as being done rather than stranding the budget.
                if (data.numMobAffixesCanAdd > 0 && data.affixesTaken.size() < 9) {
                    data.regenAffixOffers();
                }

                if (data.numMobAffixesCanAdd < 1 || data.affixOffers.size() < 3) {
                    data.affixOffers = new ArrayList<>();
                    // never strand budget on the 9 curse cap - the guard above would make every
                    // further accept a no-op while the altar gate still saw a pending pick
                    data.numMobAffixesCanAdd = 0;
                    data.consumePendingAltar(p);
                }
            }
        }

        if (data.numMobAffixesCanAdd > 0 && !data.affixOffers.isEmpty()) {
            ProphecyAltarBlock.openCurseScreen(p); // syncs as well
        } else {
            Load.player(p).playerDataSync.setDirty();
        }
    }

    @Override
    public MyPacket<AcceptProphecyPacket> newInstance() {
        return new AcceptProphecyAffixPacket("");
    }
}
