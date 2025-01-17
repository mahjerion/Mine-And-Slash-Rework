package com.robertx22.orbs_of_crafting.register;

import com.robertx22.addons.orbs_of_crafting.currency.base.ExileCurrencyItem;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemMods;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqs;
import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.SlashDeferred;

public class Orbs extends ExileKeyHolder<ExileCurrency> {

    public static Orbs INSTANCE = (Orbs) new Orbs(MMORPG.REGISTER_INFO)
            // todo is this a good way to check? I'm thinking note 1 layer of dep
            .itemIds(new ItemIdProvider(x -> SlashRef.id("currency/" + x)))
            .createItems(new ItemCreator<ExileCurrency>(x -> new ExileCurrencyItem(x.get())), x -> RegObj.register(x.itemID(), x.item(), SlashDeferred.ITEMS))
            .dependsOn(() -> ItemMods.INSTANCE)
            .dependsOn(() -> ItemReqs.INSTANCE);

    public Orbs(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    /*
    public ExileKey<ExileCurrency, IdKey> LEGENDARY_TOOL_ENCHANT = ExileCurrency.Builder.of("legendary_enchant_tool", "Orb of Legendary Tool Enchant", WorksOnBlock.ItemType.ANY)
            .addRequirement(Requirements.INSTANCE.IS_TOOL_TAG)
            .addRequirement(Requirements.INSTANCE.HAS_NO_ENCHANTS)
            .addAlwaysUseModification(Modifications.INSTANCE.ENCHANT_30_LEVELS)
            .potentialCost(0)
            .weight(0)
            .build(this);

     */


    @Override
    public void loadClass() {

    }
}
