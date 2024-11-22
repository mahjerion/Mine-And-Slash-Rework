package com.robertx22.mine_and_slash.database.data.profession;

import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ExplainedResult {

    public boolean can;
    public Component answer;

    private ExplainedResult(boolean can, Component answer) {
        this.can = can;
        this.answer = answer;
    }


    public static MutableComponent createErrorAndReason(IAutoLocName error, IAutoLocName reason) {
        var er = error.locName().withStyle(ChatFormatting.RED);
        var re = reason.locName().withStyle(ChatFormatting.YELLOW);
        return Chats.ERROR_TYPE_AND_REASON.locName(er, re);
    }

    public static ExplainedResult success() {
        return new ExplainedResult(true, Component.empty());
    }

    public static ExplainedResult failure(Component txt) {
        return new ExplainedResult(false, Component.empty().append(txt).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    public static ExplainedResult success(Component txt) {
        return new ExplainedResult(true, Component.empty().append(txt).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
    }

    public static ExplainedResult silentlyFail() {
        return new ExplainedResult(false, null);
    }
}
