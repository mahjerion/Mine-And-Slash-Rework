package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.GearModification;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.AffixData;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class CommonToMythicGuaranteedTagItemMod extends GearModification {

    public static class Data {
        public String tag_guid;

        public Data() {
        }

        public Data(String tag_guid) {
            this.tag_guid = tag_guid;
        }
    }

    public Data data;

    public CommonToMythicGuaranteedTagItemMod(String id, Data data) {
        super(ItemModificationSers.COMMON_TO_MYTHIC_GUARANTEED_TAG, id);
        this.data = data;
    }

    @Override
    public void modifyGear(ExileStack stack, ItemModificationResult r) {
        stack.get(StackKeys.GEAR).edit(gear -> {

            GearRarity oldRarity = gear.getRarity();
            gear.rar = IRarity.MYTHIC_ID;
            GearRarity newRarity = gear.getRarity();

            gear.baseStats.p = UpgradeRarityItemMod.uniformRescaleInt(gear.baseStats.p, oldRarity.base_stat_percents, newRarity.base_stat_percents);

            gear.affixes.pre.clear();
            gear.affixes.suf.clear();

            AffixData guaranteed = AffixData.rollGuaranteedTagAffix(gear, data.tag_guid);

            if (guaranteed != null) {
                guaranteed.setMaxPossibleTier(newRarity);
                guaranteed.RerollNumbers();
                gear.affixes.add(guaranteed);
            } else {
                // tag doesn't exist on any eligible affix — fall back to a normal random affix
                ExileLog.get().warn("No affix found for tag: " + data.tag_guid + ", falling back to random affix");
                gear.affixes.addOneRandomAffix(gear);
            }

            for (int affixesToAdd = newRarity.getAffixAmount() - gear.affixes.getNumberOfAffixes(); affixesToAdd > 0; affixesToAdd--) {
                gear.affixes.addOneRandomAffix(gear);
            }
        });
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return CommonToMythicGuaranteedTagItemMod.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Turns Common Item into Mythic Item and guarantees a Mythic affix with a specific tag"));
    }
}