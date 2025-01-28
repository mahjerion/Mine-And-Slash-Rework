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

public class DowngradeAffixItemMod extends GearModification {

    public Data data;


    public static record Data(UpgradeAffixItemMod.AffixFinderData finder_data) {
    }

    public DowngradeAffixItemMod(String id, Data data) {
        super(ItemModificationSers.DOWNGRADE_AFFIX_RARITY, id);
        this.data = data;
    }

    @Override
    public void modifyGear(ExileStack stack, ItemModificationResult r) {
        stack.get(StackKeys.GEAR).edit(gear -> {
            data.finder_data.finder().getAffix(gear.affixes.getPrefixesAndSuffixes(), data.finder_data).ifPresent(affix -> {
                affix.downgradeRarity();
            });
        });

    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.BAD;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return DowngradeAffixItemMod.class;
    }


    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.finder_data.finder().getTooltip(data.finder_data));
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Downgrades Rarity and re-rolls Numbers of %1$s"));
    }

}
