package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.omen;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.OmenModification;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.omen.OmenData;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarityType;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.AffixData;
import net.minecraft.network.chat.MutableComponent;

public class UpgradeOmenRarityItemMod extends OmenModification {

    public UpgradeOmenRarityItemMod(String id) {
        super(ItemModificationSers.UPGRADE_OMEN_RARITY, id);
    }

    @Override
    public void modifyOmen(ExileStack stack) {
        stack.get(StackKeys.OMEN).edit(omen -> {
            GearRarity current = omen.getRarity();

            var higher = ExileDB.GearRarities().getFilterWrapped(x -> x.type == GearRarityType.NORMAL && x.isHigherThan(current)).list;
            if (higher.isEmpty()) {
                return; // already at the highest rarity, nothing to upgrade to
            }
            // uniform pick: weighted random() would bias hard toward the next tier via drop weights
            GearRarity newRar = RandomUtils.randomFromList(higher);

            omen.rar = newRar.GUID();

            // keep stored affixes in sync with the new rarity so their magnitude reflects the upgrade
            int newPerc = OmenData.getStatPercent(omen.rarities, omen.slot_req, newRar);
            for (AffixData aff : omen.aff) {
                aff.rar = newRar.GUID();
                aff.p = newPerc;
            }
        });
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return UpgradeOmenRarityItemMod.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Upgrades the Omen to a random higher rarity"));
    }
}
