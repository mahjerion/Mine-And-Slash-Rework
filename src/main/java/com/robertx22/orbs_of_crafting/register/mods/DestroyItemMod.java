package com.robertx22.orbs_of_crafting.register.mods;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import com.robertx22.orbs_of_crafting.register.mods.base.VanillaItemModSers;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class DestroyItemMod extends ItemModification {

    public DestroyItemMod(String id) {
        super(VanillaItemModSers.DESTROY_ITEM, id);
    }


    @Override
    public Class<?> getClassForSerialization() {
        return DestroyItemMod.class;
    }


    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {

    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.BAD;
    }

    @Override
    public void applyMod(Player p, StackHolder stack, ItemModificationResult r) {
        stack.stack = Items.COAL.getDefaultInstance();
    }


    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, "DESTROYS the Item!"));
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

}
