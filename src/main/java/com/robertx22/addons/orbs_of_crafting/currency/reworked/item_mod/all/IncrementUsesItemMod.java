package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.all;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.custom.MaximumUsesReq;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.item_classes.rework.DataKey;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class IncrementUsesItemMod extends ItemModification {

    String use_key;

    public IncrementUsesItemMod(String id, MaximumUsesReq.Data data) {
        super(ItemModificationSers.INCREMENT_USES, id);
        this.use_key = data.use_id();
    }

    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {
        ExileStack ex = ExileStack.of(stack.stack);

        ex.get(StackKeys.CUSTOM).edit(gear -> {
            var key = new DataKey.IntKey(use_key);
            int uses = gear.data.get(key) + 1;
            gear.data.set(key, uses);
        });

        stack.stack = ex.getStack();
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.NEUTRAL;
    }


    @Override
    public Class<?> getClassForSerialization() {
        return IncrementUsesItemMod.class;
    }


    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, "Increments Uses"));
    }

}
