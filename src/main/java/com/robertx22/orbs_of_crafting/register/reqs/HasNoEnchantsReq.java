package com.robertx22.orbs_of_crafting.register.reqs;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.main.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class HasNoEnchantsReq extends ItemRequirement {
    public HasNoEnchantsReq(String id) {
        super(ItemReqSers.HAS_NO_ENCHANTS, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return HasNoEnchantsReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, "Must have no Enchantments")
                );
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        return !obj.stack.isEnchanted();
    }

    
}
