package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod;

import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;

public abstract class SkillGemModification extends ItemModification {

    public SkillGemModification(String serializer, String id) {
        super(serializer, id);
    }

    public abstract void modifySkillGem(ExileStack data);

    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {
        ExileStack ex = ExileStack.of(stack.stack);

        var data = ex.get(StackKeys.SKILL_GEM).get();

        if (data != null) {
            modifySkillGem(ex);
        }
        stack.stack = ex.getStack();
    }
}