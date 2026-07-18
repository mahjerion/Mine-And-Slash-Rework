package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear.ExtractSocketItemMod;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.GearRequirement;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class HasNothingSocketedReq extends GearRequirement {

    public HasNothingSocketedReq(String id) {
        super(ItemReqSers.HAS_NOTHING_SOCKETED, id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return HasNothingSocketedReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Must not have a Socketed Gem or Rune"));
    }

    @Override
    public boolean isGearValid(ItemStack stack) {
        ExileStack ex = ExileStack.of(stack);

        var gear = ex.get(StackKeys.GEAR).get();

        if (gear.sockets == null) {
            return true;
        }

        boolean hasGem = gear.sockets.lastFilledSocketGemIndex(ExtractSocketItemMod.SocketedType.GEM) > -1;
        boolean hasRune = gear.sockets.lastFilledSocketGemIndex(ExtractSocketItemMod.SocketedType.RUNE) > -1;

        return !hasGem && !hasRune;
    }
}