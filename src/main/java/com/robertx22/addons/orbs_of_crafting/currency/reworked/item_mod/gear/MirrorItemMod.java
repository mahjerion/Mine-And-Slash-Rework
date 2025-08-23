package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class MirrorItemMod extends ItemModification {
    public MirrorItemMod(String id) {
        super(ItemModificationSers.MIRROR, id);
    }

    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {
        ExileStack mirroredStack = ExileStack.of(stack.stack);
        mirroredStack.get(StackKeys.CUSTOM).edit(x -> x.data.set(CustomItemData.KEYS.MIRRORED, true));
        mirroredStack.get(StackKeys.POTENTIAL).edit(e -> e.potential = 0);
        r.extraItemsCreated.add(mirroredStack.getStack());
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Creates an unmodifiable Mirrored copy of the Item"));
    }

    @Override
    public Class<?> getClassForSerialization() {
        return MirrorItemMod.class;
    }


}
