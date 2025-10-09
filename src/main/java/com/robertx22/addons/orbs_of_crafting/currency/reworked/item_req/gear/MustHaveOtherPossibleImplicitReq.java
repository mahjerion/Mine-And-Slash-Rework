package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.GearRequirement;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.data.requirements.bases.GearRequestedFor;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.ImplicitStatsData;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class MustHaveOtherPossibleImplicitReq extends GearRequirement {

    public MustHaveOtherPossibleImplicitReq(String id) {
        super(ItemReqSers.HAS_IMPLICIT, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return MustHaveOtherPossibleImplicitReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }


    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Must have another possible implicit")
                );
    }

    @Override
    public boolean isGearValid(ItemStack stack) {
        ExileStack ex = ExileStack.of(stack);

        var gear = ex.get(StackKeys.GEAR).get();

        return gear.imp != null && !ExileDB.Affixes().getFilterWrapped(x ->
            x.GUID() != gear.imp.imp &&
            x.type == Affix.AffixSlot.implicit &&
            x.meetsRequirements(new GearRequestedFor(gear))).list.isEmpty();
    }
}
