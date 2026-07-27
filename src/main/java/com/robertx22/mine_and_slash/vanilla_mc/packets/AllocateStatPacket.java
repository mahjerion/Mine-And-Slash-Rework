package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.database.data.game_balance_config.PlayerPointsType;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.datapacks.stats.CoreStat;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class AllocateStatPacket extends MyPacket<AllocateStatPacket> {

    // shift clicking allocates multiple points at once, the server never allocates more than this per packet
    public static final int MAX_ALLOCATE_AT_ONCE = 5;

    public String stat;
    public int amount = 1;
    AllocateStatPacket.ACTION action;

    public enum ACTION {
        ALLOCATE, REMOVE
    }

    public AllocateStatPacket() {

    }

    public AllocateStatPacket(Stat stat, ACTION act) {
        this(stat, act, 1);
    }

    public AllocateStatPacket(Stat stat, ACTION act, int amount) {
        this.stat = stat.GUID();
        this.action = act;
        this.amount = amount;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "stat_alloc");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        stat = tag.readUtf(30);
        action = tag.readEnum(AllocateStatPacket.ACTION.class);
        amount = tag.readVarInt();

    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeUtf(stat, 30);
        tag.writeEnum(action);
        tag.writeVarInt(amount);

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {

        var player = ctx.getPlayer();

        // unregistered ids fall back to EmptyStat, so this also rejects a made up stat id
        if (!(ExileDB.Stats().get(stat) instanceof CoreStat)) {
            return;
        }

        Load.Unit(player).setEquipsChanged();

        PlayerData cap = Load.player(player);

        if (action == ACTION.ALLOCATE) {
            // never trust the amount the client sent
            int times = Mth.clamp(this.amount, 1, MAX_ALLOCATE_AT_ONCE);

            for (int i = 0; i < times; i++) {
                // free points are derived from the allocated map, so each put lowers them and this
                // stops on its own once the player runs out
                if (PlayerPointsType.STATS.getFreePoints(player) < 1) {
                    break;
                }
                cap.statPoints.map.put(stat, 1 + cap.statPoints.map.getOrDefault(stat, 0));
            }
        } else {
            if (PlayerPointsType.STATS.getResetPoints(player) > 0) {
                int current = cap.statPoints.map.getOrDefault(stat, 0);
                if (current > 0) {
                    // only spend the respec point when a point actually came out
                    PlayerPointsType.STATS.reduceResetPoints(player, 1);
                    cap.statPoints.map.put(stat, current - 1);
                }
            }
        }
        Load.Unit(player).setEquipsChanged();
        cap.playerDataSync.setDirty();
    }

    @Override
    public MyPacket<AllocateStatPacket> newInstance() {
        return new AllocateStatPacket();
    }
}
