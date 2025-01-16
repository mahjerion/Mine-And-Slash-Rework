package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.GearModification;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class ModifyUniqueStatsItemMod extends GearModification {


    public Data data;

    public static record Data(int num) {
    }

    public ModifyUniqueStatsItemMod(String id, Data data) {
        super(ItemModificationSers.MODIFY_UNIQUE_STATS, id);
        this.data = data;
    }

    @Override
    public void modifyGear(ExileStack stack, ItemModificationResult r) {
        stack.get(StackKeys.GEAR).edit(gear -> {
            gear.uniqueStats.increaseAllBy(gear, data.num);
        });
    }

    @Override
    public Class<?> getClassForSerialization() {
        return ModifyUniqueStatsItemMod.class;
    }


    @Override
    public OutcomeType getOutcomeType() {
        return data.num > 0 ? OutcomeType.GOOD : OutcomeType.BAD;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.num);
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        String term = "Adds";
        if (data.num < 0) {
            term = "Reduces";
        }
        term += " %1$s Percent to Unique Stats";

        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, term));
    }

    
}
