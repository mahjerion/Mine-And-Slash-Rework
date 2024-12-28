package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.soul;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.mine_and_slash.database.data.profession.items.CraftedSoulItem;
import com.robertx22.mine_and_slash.saveclasses.stat_soul.StatSoulData;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.orbs_of_crafting.main.StackHolder;
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
        return this.getDescParams(data.gear_tag);
    }


    @Override
    public Class<?> getClassForSerialization() {
        return ForceGearSlotSoulMod.class;
    }


    @Override
    public String locDescForLangFile() {
        return "Forces Soul to Produce %1$s";
    }
}
