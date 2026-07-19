package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class IsSkillGemRarityReq extends ItemRequirement {

    public Data data;

    public static record Data(String rarity) {
    }

    public IsSkillGemRarityReq(String id, Data data) {
        super("is_skill_gem_rarity", id);
        this.data = data;
    }

    public GearRarity getRarity() {
        return ExileDB.GearRarities().get(data.rarity);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsSkillGemRarityReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(getRarity().locName().withStyle(getRarity().textFormatting()));
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Must be %1$s Rarity"));
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        ExileStack ex = ExileStack.of(obj.stack);
        return ex.get(StackKeys.SKILL_GEM).hasAndTrue(x -> x.rar.equals(data.rarity));
    }
}