package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.skill_gem;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.SkillGemModification;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;

public class RerollSkillGemStatsItemMod extends SkillGemModification {

    public RerollSkillGemStatsItemMod(String id) {
        super(ItemModificationSers.REROLL_SKILL_GEM_STATS, id);
    }

    @Override
    public void modifySkillGem(ExileStack stack) {
        stack.get(StackKeys.SKILL_GEM).edit(gem -> {
            gem.perc = gem.getRarity().stat_percents.random();
        });
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return RerollSkillGemStatsItemMod.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Rerolls the stat value of the Skill Gem"));
    }
}