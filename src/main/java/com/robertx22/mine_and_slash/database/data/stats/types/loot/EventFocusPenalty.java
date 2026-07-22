package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

// Scalable Singular Focus penalty: this single stat reduces the weight of EVERY map event equally
// (subtracted in the GET_MAP_CONTENT_WEIGHT_BONUS listener). A Singular Focus node pairs it with an
// equal boost to its own event's chance, so the focused event nets to unpenalized while every other
// event is reduced. New events added later are covered automatically - no need to edit focus nodes.
public class EventFocusPenalty extends Stat {

    private EventFocusPenalty() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.RED.getName();
    }

    public static EventFocusPenalty getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public boolean IsPercent() {
        return true;
    }

    @Override
    public Elements getElement() {
        return null;
    }

    @Override
    public String locDescForLangFile() {
        return "Reduces the chance of all other Map events. Only applies if YOU start the Map.";
    }

    @Override
    public String GUID() {
        return "event_focus_penalty";
    }

    @Override
    public String locNameForLangFile() {
        return "Reduced Other Event Chance";
    }

    private static class SingletonHolder {
        private static final EventFocusPenalty INSTANCE = new EventFocusPenalty();
    }
}
