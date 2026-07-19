package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.item_types;

import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import net.minecraft.world.entity.player.Player;

public class BeSupportGemReq extends BeItemTypeRequirement {
    public BeSupportGemReq() {
        super("is_support_gem", "Must be a Support Gem");
    }

    @Override
    public Class<?> getClassForSerialization() {
        return BeSupportGemReq.class;
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        if (!StackSaving.SKILL_GEM.has(obj.stack)) {
            return false;
        }

        SkillGemData data = StackSaving.SKILL_GEM.loadFrom(obj.stack);

        return data != null && data.type == SkillGemData.SkillGemType.SUPPORT;
    }
}