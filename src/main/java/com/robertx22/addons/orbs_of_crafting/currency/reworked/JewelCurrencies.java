package com.robertx22.addons.orbs_of_crafting.currency.reworked;

import com.robertx22.addons.orbs_of_crafting.currency.base.CodeCurrency;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemMods;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqs;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.MaxUsesKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolderSection;
import com.robertx22.library_of_exile.registry.helpers.IdKey;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import com.robertx22.orbs_of_crafting.register.Modifications;

public class JewelCurrencies extends ExileKeyHolderSection<ExileCurrencies> {
    public JewelCurrencies(ExileCurrencies holder) {
        super(holder);
    }

    public ExileKey<ExileCurrency, IdKey> JEWEL_CORRUPT = ExileCurrency.Builder.of("jewel_corrupt", "Orb of Mesmerizing Chaos", ItemReqs.INSTANCE.IS_JEWEL)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .rarity(IRarity.UNIQUE_ID)
            .addModification(ItemMods.INSTANCE.JEWEL_CORRUPTION, 50)
            .addModification(Modifications.INSTANCE.DESTROY_ITEM, 50)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.RARE)
            .build(get());

    public ExileKey<ExileCurrency, IdKey> JEWEL_UPGRADE_AFFIX = ExileCurrency.Builder.of("jewel_upgrade_affix", "Orb of Glimmering Light", ItemReqs.INSTANCE.IS_JEWEL)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .rarity(IRarity.EPIC_ID)
            .addModification(ItemMods.INSTANCE.UPGRADE_JEWEL_AFFIX_RARITY, 85)
            .addModification(Modifications.INSTANCE.DESTROY_ITEM, 15)
            .potentialCost(3)
            .weight(CodeCurrency.Weights.RARE)
            .build(get());

    public ExileKey<ExileCurrency, IdKey> JEWEL_UPGRADE_AFFIX_SURE = ExileCurrency.Builder.of("jewel_sure_upgrade", "Orb of Mystery", ItemReqs.INSTANCE.IS_JEWEL)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .rarity(IRarity.MYTHIC_ID)
            .addModification(ItemMods.INSTANCE.UPGRADE_JEWEL_AFFIX_RARITY, 90)
            .addModification(Modifications.INSTANCE.DO_NOTHING, 10)
            .potentialCost(10)
            .edit(MaxUsesKey.ofUses(ItemReqs.Datas.MAX_JEWEL_UPGRADE_USES.toKey()))
            .weight(CodeCurrency.Weights.MEGA_UBER)
            .build(get());


    @Override
    public void init() {

    }
}
