package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.skill_gem;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.SkillGemModification;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear.UpgradeRarityItemMod;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.MinMax;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;

public class UpgradeSkillGemRarityItemMod extends SkillGemModification {

    public UpgradeSkillGemRarityItemMod(String id) {
        super(ItemModificationSers.UPGRADE_SKILL_GEM_RARITY, id);
    }

    @Override
    public void modifySkillGem(ExileStack stack) {
        stack.get(StackKeys.SKILL_GEM).edit(gem -> {
            GearRarity current = gem.getRarity();
            GearRarity higher = current.getHigherRarity();

            MinMax oldRange = current.stat_percents;
            MinMax newRange = higher.stat_percents;

            gem.perc = UpgradeRarityItemMod.uniformRescaleInt(gem.perc, oldRange, newRange);
            gem.rar = higher.GUID();
        });
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return UpgradeSkillGemRarityItemMod.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Upgrades the rarity of the Skill Gem"));
    }
}