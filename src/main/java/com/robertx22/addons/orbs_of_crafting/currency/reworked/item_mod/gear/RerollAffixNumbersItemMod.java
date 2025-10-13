package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear;

import java.util.List;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.GearModification;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.AffixData;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class RerollAffixNumbersItemMod extends GearModification {
    public enum AffixFinder {
        AFFIX(Words.AFFIX) {
            @Override
            public List<AffixData> getAffixes(List<AffixData> affixes) {
                return affixes;
            }
        },
        PREFIX(Words.PREFIX) {
            @Override
            public List<AffixData> getAffixes(List<AffixData> affixes) {
                return affixes.stream().filter(x -> x.ty.isPrefix()).toList();
            }
        },
        SUFFIX(Words.SUFFIX) {
            @Override
            public List<AffixData> getAffixes(List<AffixData> affixes) {
                return affixes.stream().filter(x -> x.ty.isSuffix()).toList();
            }
        };

        AffixFinder(Words word) {
            this.word = word;
        }

        private Words word;

        protected Words getWordINTERNAL() {
            return word;
        }

        public MutableComponent getTooltip() {
            return word.locName();
        }

        public abstract List<AffixData> getAffixes(List<AffixData> affixes);
    }

    public AffixFinder data;

    public RerollAffixNumbersItemMod(String id, AffixFinder data) {
        super(ItemModificationSers.REROLL_AFFIX_NUMBERS, id);
        this.data = data;
    }

    @Override
    public void modifyGear(ExileStack stack, ItemModificationResult r) {
        stack.get(StackKeys.GEAR).edit(gear -> {
            getData().getAffixes(gear.affixes.getPrefixesAndSuffixes()).forEach(affix -> {
                affix.RerollNumbers();
            });
        });
    }


    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(getData().getTooltip());
    }


    @Override
    public Class<?> getClassForSerialization() {
        return RerollAffixNumbersItemMod.class;
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Re-rolls %1$s Numbers"));
    }

    protected AffixFinder getData()
    {
        // Legacy support
        return data != null ? data : AffixFinder.AFFIX;
    }
}
