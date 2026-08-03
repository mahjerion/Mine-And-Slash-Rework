package com.robertx22.mine_and_slash.database.data.spells.components.actions;

import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;

// a named pool of summoned blocks. only x of them can be alive per caster at once,
// x being whatever the linked stat put into the spell's event data.
// a single cast can also place several of them at once, see extraCountEventDataKey.
public enum BlockSummonLimitGroup {

    TOTEM("totem", EventData.MAX_TOTEMS, EventData.EXTRA_TOTEMS),
    BANNER("banner", EventData.MAX_BANNERS, EventData.EXTRA_BANNERS);

    public final String id;
    public final String maxEventDataKey;
    public final String extraCountEventDataKey;

    BlockSummonLimitGroup(String id, String maxEventDataKey, String extraCountEventDataKey) {
        this.id = id;
        this.maxEventDataKey = maxEventDataKey;
        this.extraCountEventDataKey = extraCountEventDataKey;
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
