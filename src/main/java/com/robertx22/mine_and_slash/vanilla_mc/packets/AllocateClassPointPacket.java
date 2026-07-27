package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.spell_school.SpellSchool;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class AllocateClassPointPacket extends MyPacket<AllocateClassPointPacket> {

    // shift clicking allocates multiple points at once, the server never allocates more than this per packet
    public static final int MAX_ALLOCATE_AT_ONCE = 4;

    public String id;
    public String schoolid;
    public int amount = 1;
    AllocateClassPointPacket.ACTION action;

    public enum ACTION {
        ALLOCATE, REMOVE
    }

    public AllocateClassPointPacket() {

    }

    public AllocateClassPointPacket(SpellSchool school, Perk perk, ACTION action) {
        this(school, perk, action, 1);
    }

    public AllocateClassPointPacket(SpellSchool school, Perk perk, ACTION action, int amount) {
        this.id = perk.GUID();
        this.schoolid = school.GUID();
        this.action = action;
        this.amount = amount;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "spell_alloc");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        id = tag.readUtf(100);
        schoolid = tag.readUtf(100);
        action = tag.readEnum(AllocateClassPointPacket.ACTION.class);
        amount = tag.readVarInt();

    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeUtf(id, 100);
        tag.writeUtf(schoolid, 100);
        tag.writeEnum(action);
        tag.writeVarInt(amount);

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {

        var player = ctx.getPlayer();

        if (!ExileDB.Perks().isRegistered(this.id) || !ExileDB.SpellSchools().isRegistered(this.schoolid)) {
            return;
        }

        Perk perk = ExileDB.Perks().get(this.id);
        SpellSchool school = ExileDB.SpellSchools().get(this.schoolid);

        // the perk has to actually belong to the school sent, otherwise the level requirements of a
        // different school could be used to allocate it
        if (!school.perks.containsKey(perk.GUID())) {
            return;
        }

        var data = Load.player(player).ascClass;

        if (action == ACTION.ALLOCATE) {

            // never trust the amount the client sent
            int times = Mth.clamp(this.amount, 1, MAX_ALLOCATE_AT_ONCE);

            for (int i = 0; i < times; i++) {
                // rechecked for every single point, free points are derived from the allocated levels,
                // so this can never spend more points than the player has
                var res = data.canLearn(player, school, perk);
                if (!res.can) {
                    if (i == 0) {
                        player.sendSystemMessage(res.answer);
                    }
                    break;
                }
                data.learn(perk, school);
            }
        } else {
            if (data.canUnlearn(player, school, perk)) {
                data.unlearn(player, perk, school);
            }
        }

        Load.Unit(player).setEquipsChanged();


        Load.player(player).playerDataSync.setDirty();

    }

    @Override
    public MyPacket<AllocateClassPointPacket> newInstance() {
        return new AllocateClassPointPacket();
    }
}

