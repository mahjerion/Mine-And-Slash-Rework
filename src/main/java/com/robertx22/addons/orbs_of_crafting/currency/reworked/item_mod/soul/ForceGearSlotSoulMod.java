package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.soul;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.profession.items.CraftedSoulItem;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.stat_soul.StatSoulData;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class ForceGearSlotSoulMod extends ItemModification {

    public ForceGearSlotSoulMod.Data data;

    public static record Data(String gear_tag) {
    }

    public ForceGearSlotSoulMod(String id, ForceGearSlotSoulMod.Data data) {
        super(ItemModificationSers.FORCE_SOUL_TAG, id);
        this.data = data;
    }


    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {

        // todo i should merge souls into 1 type of data saving..
        // maybe add profession outcome types, default type being tier 1 and 2 etc?
        // can also leave it as is, souls probably wont get any more specific currencies


        var craftedStack = stack.stack;

        StatSoulData soul = StackSaving.STAT_SOULS.loadFrom(craftedStack);
        if (soul != null) {
            soul.force_tag = this.data.gear_tag;
            soul.saveToStack(craftedStack);
        } else {
            if (craftedStack.getItem() instanceof CraftedSoulItem i) {
                var craftedSoul = i.getSoul(craftedStack);
                if (craftedSoul != null) {
                    craftedStack.getOrCreateTag().putString("force_tag", this.data.gear_tag);
                }
            }
        }
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.gear_tag);
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Forces Soul to Produce %1$s"));
    }


    @Override
    public Class<?> getClassForSerialization() {
        return ForceGearSlotSoulMod.class;
    }


}
