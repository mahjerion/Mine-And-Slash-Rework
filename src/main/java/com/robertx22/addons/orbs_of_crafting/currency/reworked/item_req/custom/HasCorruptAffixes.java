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

public class HasCorruptAffixes extends ItemRequirement {

    public HasCorruptAffixes(String id) {
        super(ItemReqSers.HAS_CORRUPTION_AFFIXES, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return HasCorruptAffixes.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public boolean isValid(Player p, StackHolder s) {
        ExileStack ex = ExileStack.of(s.stack);

        if (ex.get(StackKeys.GEAR).has()) {
            return !ex.get(StackKeys.GEAR).get().affixes.cor.isEmpty();
        }
        if (ex.get(StackKeys.JEWEL).has()) {
            return !ex.get(StackKeys.JEWEL).get().cor.isEmpty();
        }

        return false;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, "Must have Corruption affixes")
                );
    }


}
