package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.jewel;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.JewelModification;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;

// todo probably turn corruption stats into its own component later on
public class CorruptJewelItemMod extends JewelModification {
    public CorruptJewelItemMod(String id) {
        super(ItemModificationSers.JEWEL_CORRUPT, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return CorruptJewelItemMod.class;
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public void modifyJewel(ExileStack data) {
        data.get(StackKeys.JEWEL).edit(x -> x.corrupt());
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, "Corrupts the Jewel"));
    }
}
