package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.GearModification;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class AddGearLevelItemMod extends GearModification {


    public Data data;

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(SlashRef.MODID, this, "Adds %1$s Gear Levels")
                );
    }

    public static record Data(int add_levels) {
    }

    public AddGearLevelItemMod(String id, Data data) {
        super(ItemModificationSers.ADD_LEVEL, id);
        this.data = data;
    }

    @Override
    public void modifyGear(ExileStack stack, ItemModificationResult r) {
        stack.get(StackKeys.GEAR).edit(gear -> {
            gear.lvl += data.add_levels;

            if (gear.lvl > GameBalanceConfig.get().MAX_LEVEL) {
                gear.lvl = GameBalanceConfig.get().MAX_LEVEL;
            }
        });
    }

    @Override
    public Class<?> getClassForSerialization() {
        return AddGearLevelItemMod.class;
    }


    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.add_levels);
        // return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.add_levels);
    }

}
