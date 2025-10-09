package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.GearModification;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.data.requirements.bases.GearRequestedFor;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class RerollImplicitAffixItemMod extends GearModification {


    public RerollImplicitAffixItemMod(String id) {
        super(ItemModificationSers.REROLL_IMPLICIT_AFFIX, id);
    }

    @Override
    public void modifyGear(ExileStack stack, ItemModificationResult r) {
        stack.get(StackKeys.GEAR).edit(gear -> {
            // Don't roll the same implicit we already have
            var opt = ExileDB.Affixes().getFilterWrapped(x ->
                x.GUID() != gear.imp.imp &&
                x.type == Affix.AffixSlot.implicit &&
                x.meetsRequirements(new GearRequestedFor(gear)));

            if (!opt.list.isEmpty()) {
                gear.imp.imp = opt.random().GUID();
            }

            gear.imp.RerollNumbers(gear);
        });
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
    public Class<?> getClassForSerialization() {
        return RerollImplicitAffixItemMod.class;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Re-rolls Implicit Affix"));
    }
}
