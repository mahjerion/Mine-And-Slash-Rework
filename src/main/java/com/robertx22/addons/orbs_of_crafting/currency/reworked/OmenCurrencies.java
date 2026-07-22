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

public class OmenCurrencies extends ExileKeyHolderSection<ExileCurrencies> {
    public OmenCurrencies(ExileCurrencies holder) {
        super(holder);
    }

    public ExileKey<ExileCurrency, IdKey> OMEN_STAT_REROLL = ExileCurrency.Builder.of("omen_stat_reroll", "Orb of Foresight", ItemReqs.INSTANCE.IS_OMEN)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.RARE_ID)
            .addModification(ItemMods.INSTANCE.REROLL_OMEN_STATS, 75)
            .addModification(Modifications.INSTANCE.DESTROY_ITEM, 25)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.COMMON)
            .build(get());

    public ExileKey<ExileCurrency, IdKey> OMEN_RARITY_RANDOM_UPGRADE = ExileCurrency.Builder.of("omen_rarity_random_upgrade", "Orb of Knowledge", ItemReqs.INSTANCE.IS_OMEN)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.OMEN_HAS_HIGHER_RARITY)
            .rarity(IRarity.LEGENDARY_ID)
            .addAlwaysUseModification(ItemMods.INSTANCE.UPGRADE_OMEN_RARITY)
            .edit(MaxUsesKey.ofUses(ItemReqs.Datas.MAX_OMEN_RARITY_USES.toKey()))
            .potentialCost(0)
            .weight(CodeCurrency.Weights.UBER)
            .build(get());


    @Override
    public void init() {

    }
}
