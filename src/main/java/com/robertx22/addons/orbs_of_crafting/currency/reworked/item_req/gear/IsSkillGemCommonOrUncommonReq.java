package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class IsSkillGemCommonOrUncommonReq extends ItemRequirement {

    public IsSkillGemCommonOrUncommonReq(String id) {
        super("is_skill_gem_common_or_uncommon", id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsSkillGemCommonOrUncommonReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Must be Common Or Uncommon"));
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        ExileStack ex = ExileStack.of(obj.stack);
        return ex.get(StackKeys.SKILL_GEM).hasAndTrue(x ->
                x.rar.equals(IRarity.COMMON_ID) || x.rar.equals(IRarity.UNCOMMON));
    }
}