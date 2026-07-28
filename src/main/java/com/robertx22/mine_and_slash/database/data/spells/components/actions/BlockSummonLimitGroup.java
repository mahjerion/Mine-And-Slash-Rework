package com.robertx22.mine_and_slash.database.data.spells.components.actions;

import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;

// a named pool of summoned blocks. only x of them can be alive per caster at once,
// x being whatever the linked stat put into the spell's event data.
public enum BlockSummonLimitGroup {

    TOTEM("totem", EventData.MAX_TOTEMS),
    BANNER("banner", EventData.MAX_BANNERS);

    public final String id;
    public final String eventDataKey;

    BlockSummonLimitGroup(String id, String eventDataKey) {
        this.id = id;
        this.eventDataKey = eventDataKey;
    }

    public static BlockSummonLimitGroup fromId(String id) {
        for (BlockSummonLimitGroup group : values()) {
            if (group.id.equals(id)) {
                return group;
            }
        }
        return null;
    }
}
