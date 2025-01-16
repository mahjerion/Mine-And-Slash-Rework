package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.GearModification;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class CorruptGearItemMod extends GearModification {
    public Data data;

    public static record Data(boolean adds_affixes) {
    }

    public CorruptGearItemMod(String id, Data data) {
        super(ItemModificationSers.CORRUPT_GEAR, id);
        this.data = data;
    }

    @Override
    public void modifyGear(ExileStack stack, ItemModificationResult r) {
        stack.get(StackKeys.GEAR).edit(gear -> {
            if (data.adds_affixes) {
                // todo will this work
                var chaos = ExileDB.ChaosStats().getFilterWrapped(x -> x.for_item_rarities.contains(gear.rar)).random();
                chaos.applyToGear(stack);
            } else {
                stack.get(StackKeys.CUSTOM).edit(x -> x.data.set(CustomItemData.KEYS.CORRUPT, true));
                stack.get(StackKeys.POTENTIAL).edit(e -> e.potential = 0);
            }
        });
    }

    @Override
    public Class<?> getClassForSerialization() {
        return CorruptGearItemMod.class;
    }


    @Override
    public OutcomeType getOutcomeType() {
        return this.data.adds_affixes ? OutcomeType.GOOD : OutcomeType.BAD;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        String t = "Corrupts the Item, making it unmodifiable";
        if (data.adds_affixes) {
            t += " and adds a Random amount of Corruption Affixes";
        }
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, t));
    }


}
