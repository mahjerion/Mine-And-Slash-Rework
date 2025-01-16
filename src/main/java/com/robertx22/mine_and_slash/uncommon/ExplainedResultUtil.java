package com.robertx22.mine_and_slash.uncommon;

import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class ExplainedResultUtil {
    public static MutableComponent createErrorAndReason(IAutoLocName error, IAutoLocName reason) {
        var er = error.locName().withStyle(ChatFormatting.RED);
        var re = reason.locName().withStyle(ChatFormatting.YELLOW);
        return createErrorAndReason(er, re);
    }

    public static MutableComponent createErrorAndReason(MutableComponent er, MutableComponent re) {
        return Chats.ERROR_TYPE_AND_REASON.locName(er, re);
    }

    public static void sendErrorMessage(Player p, IAutoLocName error, IAutoLocName reason) {
        p.sendSystemMessage(createErrorAndReason(error, reason));
    }
}
