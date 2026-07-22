package com.robertx22.addons.orbs_of_crafting.currency.reworked;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemMods;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqs;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.MaxUsesKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolderSection;
import com.robertx22.library_of_exile.registry.helpers.IdKey;
import com.robertx22.mine_and_slash.tags.imp.SlotTag;
import com.robertx22.mine_and_slash.tags.all.SlotTags;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;

import java.util.List;

// The "Seed" currencies: the special, encounter-exclusive bonus reward the Imprisoned Monster
// (ImprisonedMonsterBlock) has a chance to drop on death, on top of its regular currency drops.
// Each restores Potential to a gear item, once - gated by gear slot and by the shared
// ItemReqs.Datas.SEED_USES max-uses counter (all 14 currencies key off the same "seed_uses" NBT
// counter, max 1), so a Seed (base or Perfected) can only ever be applied once per item. Weight 0:
// these never show up in the general weighted currency loot pool (CurrencyLootGen) - only this
// encounter grants them.
public class ImprisonedMonsterCurrencies extends ExileKeyHolderSection<ExileCurrencies> {
    public ImprisonedMonsterCurrencies(ExileCurrencies holder) {
        super(holder);
    }

    public ExileKey<ExileCurrency, IdKey> MIGHT_SEED = seed("might_seed", "Might Seed", SlotTags.weapon_family, ItemMods.INSTANCE.ADD_20_POTENTIAL, IRarity.RARE_ID);
    public ExileKey<ExileCurrency, IdKey> MIND_SEED = seed("mind_seed", "Mind Seed", SlotTags.helmet, ItemMods.INSTANCE.ADD_20_POTENTIAL, IRarity.RARE_ID);
    public ExileKey<ExileCurrency, IdKey> HEART_SEED = seed("heart_seed", "Heart Seed", SlotTags.chest, ItemMods.INSTANCE.ADD_20_POTENTIAL, IRarity.RARE_ID);
    public ExileKey<ExileCurrency, IdKey> ROOT_SEED = seed("root_seed", "Root Seed", SlotTags.pants, ItemMods.INSTANCE.ADD_20_POTENTIAL, IRarity.RARE_ID);
    public ExileKey<ExileCurrency, IdKey> STRIDE_SEED = seed("stride_seed", "Stride Seed", SlotTags.boots, ItemMods.INSTANCE.ADD_20_POTENTIAL, IRarity.RARE_ID);
    public ExileKey<ExileCurrency, IdKey> SOUL_SEED = seed("soul_seed", "Soul Seed", SlotTags.necklace, ItemMods.INSTANCE.ADD_20_POTENTIAL, IRarity.RARE_ID);
    public ExileKey<ExileCurrency, IdKey> BOND_SEED = seed("bond_seed", "Bond Seed", SlotTags.ring, ItemMods.INSTANCE.ADD_20_POTENTIAL, IRarity.RARE_ID);

    public ExileKey<ExileCurrency, IdKey> PERFECTED_MIGHT_SEED = seed("perfected_might_seed", "Perfected Might Seed", SlotTags.weapon_family, ItemMods.INSTANCE.ADD_50_POTENTIAL, IRarity.LEGENDARY_ID);
    public ExileKey<ExileCurrency, IdKey> PERFECTED_MIND_SEED = seed("perfected_mind_seed", "Perfected Mind Seed", SlotTags.helmet, ItemMods.INSTANCE.ADD_50_POTENTIAL, IRarity.LEGENDARY_ID);
    public ExileKey<ExileCurrency, IdKey> PERFECTED_HEART_SEED = seed("perfected_heart_seed", "Perfected Heart Seed", SlotTags.chest, ItemMods.INSTANCE.ADD_50_POTENTIAL, IRarity.LEGENDARY_ID);
    public ExileKey<ExileCurrency, IdKey> PERFECTED_ROOT_SEED = seed("perfected_root_seed", "Perfected Root Seed", SlotTags.pants, ItemMods.INSTANCE.ADD_50_POTENTIAL, IRarity.LEGENDARY_ID);
    public ExileKey<ExileCurrency, IdKey> PERFECTED_STRIDE_SEED = seed("perfected_stride_seed", "Perfected Stride Seed", SlotTags.boots, ItemMods.INSTANCE.ADD_50_POTENTIAL, IRarity.LEGENDARY_ID);
    public ExileKey<ExileCurrency, IdKey> PERFECTED_SOUL_SEED = seed("perfected_soul_seed", "Perfected Soul Seed", SlotTags.necklace, ItemMods.INSTANCE.ADD_50_POTENTIAL, IRarity.LEGENDARY_ID);
    public ExileKey<ExileCurrency, IdKey> PERFECTED_BOND_SEED = seed("perfected_bond_seed", "Perfected Bond Seed", SlotTags.ring, ItemMods.INSTANCE.ADD_50_POTENTIAL, IRarity.LEGENDARY_ID);

    // convenience lists for the reward roll in ImprisonedMonsterBlock
    public List<ExileKey<ExileCurrency, IdKey>> BASE_SEEDS = List.of(
            MIGHT_SEED, MIND_SEED, HEART_SEED, ROOT_SEED, STRIDE_SEED, SOUL_SEED, BOND_SEED);
    public List<ExileKey<ExileCurrency, IdKey>> PERFECTED_SEEDS = List.of(
            PERFECTED_MIGHT_SEED, PERFECTED_MIND_SEED, PERFECTED_HEART_SEED, PERFECTED_ROOT_SEED,
            PERFECTED_STRIDE_SEED, PERFECTED_SOUL_SEED, PERFECTED_BOND_SEED);

    private ExileKey<ExileCurrency, IdKey> seed(String id, String name, SlotTag slot, ExileKey<ItemModification, ?> restoreMod, String rarity) {
        return ExileCurrency.Builder.of(id, name, ItemReqs.INSTANCE.IS_GEAR)
                .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
                .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
                .addRequirement(ItemReqs.INSTANCE.IS_GEAR_SLOT.get(new IdKey(slot.GUID())))
                .rarity(rarity)
                .addAlwaysUseModification(restoreMod)
                .edit(MaxUsesKey.ofUses(ItemReqs.Datas.SEED_USES.toKey()))
                .potentialCost(0)
                .weight(0)
                .build(get());
    }

    @Override
    public void init() {

    }
}
