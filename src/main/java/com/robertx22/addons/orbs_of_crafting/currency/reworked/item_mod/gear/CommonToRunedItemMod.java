package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.GearModification;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class CommonToRunedItemMod extends GearModification {
    public CommonToRunedItemMod(String id) {
        super(ItemModificationSers.COMMON_TO_RUNED, id);
    }

    @Override
    public void modifyGear(ExileStack stack, ItemModificationResult r) {
        stack.get(StackKeys.GEAR).edit(gear -> {
            GearRarity oldRarity = gear.getRarity();
            gear.rar = IRarity.RUNEWORD_ID;
            GearRarity newRarity = gear.getRarity();

            // Remove affixes
            gear.affixes.pre.clear();
            gear.affixes.suf.clear();

            // Rescale base stats to upgraded roll range
            gear.baseStats.p = UpgradeRarityItemMod.uniformRescaleInt(gear.baseStats.p, oldRarity.base_stat_percents, newRarity.base_stat_percents);

            // Add rune sockets
            int sockets = newRarity.sockets.random();

            for (int index = gear.sockets.getTotalSockets(); index < sockets; index++) {
                gear.sockets.addSocket();
            }
        });
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return CommonToRunedItemMod.class;
    }


    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Turns Common Item into Runed Item, adding sockets"));
    }

}
