package com.robertx22.mine_and_slash.events;

import com.robertx22.library_of_exile.events.base.ExileEvent;
import com.robertx22.library_of_exile.events.base.ExileEventCaller;
import net.minecraft.world.entity.player.Player;

public class MineAndSlashEvents {
    public static ExileEventCaller<OnPerkUnlearnedAndRemoved> PERK_UNLEARNED_AND_REMOVED = new ExileEventCaller<>();

    public static class OnPerkUnlearnedAndRemoved extends ExileEvent {
        public String perk;
        public Player player;

        public OnPerkUnlearnedAndRemoved(Player player, String spell) {
            this.perk = spell;
            this.player = player;
        }
    }
}
