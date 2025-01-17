package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.custom;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class IsNotCorruptedReq extends ItemRequirement {

    public IsNotCorruptedReq(String id) {
        super(ItemReqSers.IS_NOT_CORRUPTED, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsNotCorruptedReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, "Must not be corrupted")
                );
    }


    @Override
    public boolean isValid(Player p, StackHolder stack) {
        ExileStack ex = ExileStack.of(stack.stack);

        if (ex.get(StackKeys.CUSTOM).hasAndTrue(x -> x.isCorrupted())) {
            return false;
        }
        return true;
    }


}
