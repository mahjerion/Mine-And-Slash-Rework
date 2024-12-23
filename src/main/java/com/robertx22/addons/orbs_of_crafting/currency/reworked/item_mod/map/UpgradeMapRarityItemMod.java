package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.map;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.MapModification;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.loot.blueprints.MapBlueprint;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationSers;
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
        return locDesc();
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
    public String locDescForLangFile() {
        return "Upgrades Map Rarity";
    }
}
