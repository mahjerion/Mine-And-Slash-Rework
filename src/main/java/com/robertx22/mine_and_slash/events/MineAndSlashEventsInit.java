package com.robertx22.mine_and_slash.events;

import static com.robertx22.mine_and_slash.events.MineAndSlashEvents.PERK_UNLEARNED_AND_REMOVED;

public class MineAndSlashEventsInit {
    public static void initEvents() {
        PERK_UNLEARNED_AND_REMOVED.register(new RemoveSummonsFromPlayer());
    }
}
