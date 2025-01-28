package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.map;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.MapModification;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.loot.blueprints.MapBlueprint;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;

public class UpgradeMapRarityItemMod extends MapModification {
    public UpgradeMapRarityItemMod(String id) {
        super(ItemModificationSers.UPGRADE_MAP_RARITY, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return UpgradeMapRarityItemMod.class;
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
    public void modifyMap(ExileStack stack) {
        stack.get(StackKeys.MAP).edit(data -> {
            data.rar = data.getRarity().getHigherRarity().GUID();
            MapBlueprint.genAffixes(data, data.getRarity());
            data.tier = data.getRarity().map_tiers.random();
        });
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Upgrades Map Rarity"));
    }

}
