package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.all;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear.UpgradeAffixItemMod;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.main.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class UpgradeCorruptionAffixMod extends ItemModification {

    UpgradeAffixItemMod.AffixFinderData data;

    public UpgradeCorruptionAffixMod(String id, UpgradeAffixItemMod.AffixFinderData data) {
        super(ItemModificationSers.UPGRADE_CORRUPT_AFFIX_RARITY, id);
        this.data = data;
    }


    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {
        ExileStack ex = ExileStack.of(stack.stack);

        ex.get(StackKeys.GEAR).editIfHas(gear -> {
            data.finder().getAffix(gear.affixes.cor, data).ifPresent(affix -> {
                affix.upgradeRarity();
            });
        });
        ex.get(StackKeys.JEWEL).editIfHas(jewel -> {
            data.finder().getAffix(jewel.cor, data).ifPresent(affix -> {
                affix.upgradeRarity();
            });
        });

        stack.stack = ex.getStack();

    }

    @Override
    public Class<?> getClassForSerialization() {
        return UpgradeCorruptionAffixMod.class;
    }


    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.finder().getTooltip(data));
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, "Upgrades Rarity of %1$s of Corruption"));
    }

}
