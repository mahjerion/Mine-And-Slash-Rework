package com.robertx22.mine_and_slash.loot.generators;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.addon.ExtendedOrb;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.ItemBlueprint;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;
import com.robertx22.orbs_of_crafting.main.OrbDatabase;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import net.minecraft.world.item.ItemStack;

public class CurrencyLootGen extends BaseLootGen<ItemBlueprint> {

    public CurrencyLootGen(LootInfo info) {
        super(info);
    }

    @Override
    public float baseDropChance() {
        float chance = (float) ServerContainer.get().CURRENCY_DROPRATE.get().floatValue();
        return chance;
    }

    @Override
    public LootType lootType() {
        return LootType.Currency;
    }

    @Override
    public boolean condition() {
        return info.level > 5;
    }

    @Override
    public ItemStack generateOne() {

        ExileCurrency currency = OrbDatabase.Currency()
                .getFilterWrapped(x -> {
                    var ext = ExtendedOrb.from(x);
                    if (ext != null) {
                        if (ext.drop_req.hasLeague() && !ext.drop_req.canDropInLeague(info.league, info.level)) {
                            return false;
                        }
                    }
                    return true;
                })
                .random();

        return currency.getItem().getDefaultInstance();

    }

}
