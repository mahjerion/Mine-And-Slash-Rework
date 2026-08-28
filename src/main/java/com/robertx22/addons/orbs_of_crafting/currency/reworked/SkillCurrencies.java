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

public class SkillCurrencies extends ExileKeyHolderSection<ExileCurrencies> {
    public SkillCurrencies(ExileCurrencies holder) {
        super(holder);
    }

    public ExileKey<ExileCurrency, IdKey> AURA_GEM_RARITY_UPGRADE = ExileCurrency.Builder.of("aura_gem_rarity_upgrade", "Orb of Ember", ItemReqs.INSTANCE.IS_AURA_GEM)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.IS_SKILL_GEM_COMMON_OR_UNCOMMON)
            .addAlwaysUseModification(ItemMods.INSTANCE.UPGRADE_SKILL_GEM_RARITY)
            .rarity(IRarity.UNCOMMON)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.UNCOMMON)
            .build(get());

    public ExileKey<ExileCurrency, IdKey> AURA_GEM_STAT_REROLL = ExileCurrency.Builder.of("aura_gem_stat_reroll", "Orb of Flames", ItemReqs.INSTANCE.IS_AURA_GEM)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addModification(ItemMods.INSTANCE.REROLL_SKILL_GEM_STATS, 75)
            .addModification(Modifications.INSTANCE.DESTROY_ITEM, 25)
            .rarity(IRarity.MYTHIC_ID)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.MYTHIC)
            .build(get());

    public ExileKey<ExileCurrency, IdKey> SUPPORT_GEM_RARITY_UPGRADE = ExileCurrency.Builder.of("support_gem_rarity_upgrade", "Orb of Shock", ItemReqs.INSTANCE.IS_SUPPORT_GEM)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.IS_SKILL_GEM_COMMON_OR_UNCOMMON)
            .addAlwaysUseModification(ItemMods.INSTANCE.UPGRADE_SKILL_GEM_RARITY)
            .rarity(IRarity.UNCOMMON)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.UNCOMMON)
            .build(get());

    public ExileKey<ExileCurrency, IdKey> SUPPORT_GEM_STAT_REROLL = ExileCurrency.Builder.of("support_gem_stat_reroll", "Orb of Tension", ItemReqs.INSTANCE.IS_SUPPORT_GEM)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addModification(ItemMods.INSTANCE.REROLL_SKILL_GEM_STATS, 75)
            .addModification(Modifications.INSTANCE.DESTROY_ITEM, 25)
            .rarity(IRarity.LEGENDARY_ID)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.LEGENDARY)
            .build(get());


    @Override
    public void init() {

    }
}
