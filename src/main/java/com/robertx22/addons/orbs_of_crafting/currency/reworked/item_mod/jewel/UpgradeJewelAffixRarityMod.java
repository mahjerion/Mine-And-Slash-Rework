package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.jewel;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.JewelModification;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear.UpgradeAffixItemMod;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import net.minecraft.network.chat.MutableComponent;

public class UpgradeJewelAffixRarityMod extends JewelModification {

    public UpgradeAffixItemMod.AffixFinderData data;


    public UpgradeJewelAffixRarityMod(String id, UpgradeAffixItemMod.AffixFinderData data) {
        super(ItemModificationSers.UPGRADE_JEWEL_AFFIX_RARITY, id);
        this.data = data;
    }

    @Override
    public void modifyJewel(ExileStack stack) {
        stack.get(StackKeys.JEWEL).edit(x -> {
            data.finder().getAffix(x.affixes, data).ifPresent(affix -> {
                affix.upgradeRarity();
            });
        });

    }

    @Override
    public ItemModification.OutcomeType getOutcomeType() {
        return ItemModification.OutcomeType.GOOD;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return UpgradeJewelAffixRarityMod.class;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, "Upgrades Rarity and re-rolls Numbers of %1$s"));
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.finder().getTooltip(data));
    }

}
