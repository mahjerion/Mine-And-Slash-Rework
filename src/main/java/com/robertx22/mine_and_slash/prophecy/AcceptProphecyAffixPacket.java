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
            if (data.numMobAffixesCanAdd > 0 && !data.isAtCurseCap()) {
                data.affixesTaken.removeIf(x -> x == null || x.isEmpty() || !ExileDB.MapAffixes().isRegistered(x));

                data.affixesTaken.add(id);
                // a curse that was just taken must never stay on the offer list, or the contains()
                // check above would let the same card be accepted twice
                data.affixOffers.remove(this.id);

                data.numMobAffixesCanAdd--;

                // Twin Curse - roll a fresh set for the next pick and put the screen back up.
                // without this the offers stay stale and the leftover budget blocks every altar in
                // the map for good.
                if (data.numMobAffixesCanAdd > 0 && !data.isAtCurseCap()) {
                    data.regenAffixOffers();
                }

                // never strand budget: the guards above turn every further accept into a no-op once
                // the cap is hit or the affix pool runs dry, while the altar gate would still see a
                // pending pick and short circuit every altar for the rest of the map
                if (data.numMobAffixesCanAdd < 1 || data.isAtCurseCap() || data.affixOffers.isEmpty()) {
                    data.affixOffers = new ArrayList<>();
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
