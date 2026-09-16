package com.robertx22.mine_and_slash.database.data.support_gem;

import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;

import java.util.List;

/**
 * The "you can't run two of these at once" rule for support gems, in one place.
 * <p>
 * Players have always been held to it by {@code SocketedGem.removeSupportGemsIfTooMany}, which had its
 * own inline copy, and mercenaries were held to it not at all - {@code MercenarySlotType.SUPPORT}
 * checked the gem type, the level requirement and the slot unlock and nothing else, so a mercenary
 * could stack three GMPs on one skill and {@code StatCalculation.collectMercGemStats} would sum all
 * three. Both paths now ask this class, so they cannot drift apart again.
 */
public class SupportGemRules {

    /** whether two support gems may sit under the same skill together */
    public static boolean conflicts(SkillGemData a, SkillGemData b) {
        SupportGem sa = a == null ? null : a.getSupport();
        SupportGem sb = b == null ? null : b.getSupport();

        if (sa == null || sb == null) {
            return false;
        }
        if (sa.id.equals(sb.id)) {
            return true;
        }
        // one_of_a_kind is a group key rather than a flag - gmp, lmp and their barrage variants all
        // share "proj_count", and only one gem out of a group may be socketed. sb needs no
        // isOneOfAKind check of its own, equality against a non empty string already implies it.
        return sa.isOneOfAKind() && sa.one_of_a_kind.equals(sb.one_of_a_kind);
    }

    /** whether candidate may join gems that are already socketed under the same skill */
    public static boolean conflicts(List<SkillGemData> socketed, SkillGemData candidate) {
        for (SkillGemData other : socketed) {
            if (conflicts(other, candidate)) {
                return true;
            }
        }
        return false;
    }
}
