package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear;

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

// Gates a currency to gear of a specific slot family, via the item's BaseGearType tags
// (e.g. SlotTags.helmet, SlotTags.weapon_family). Parametrized per-slot in ItemReqs.IS_GEAR_SLOT.
public class IsGearSlotTagReq extends GearRequirement {

    public Data data;

    public static record Data(String tag) {
    }

    public IsGearSlotTagReq(String id, Data data) {
        super(ItemReqSers.IS_GEAR_SLOT_TAG, id);
        this.data = data;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsGearSlotTagReq.class;
    }

    @Override
    public boolean isGearValid(ItemStack stack) {
        ExileStack ex = ExileStack.of(stack);
        var gear = ex.get(StackKeys.GEAR).get();
        return gear.GetBaseGearType().getTags().contains(data.tag);
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Must be the correct Gear Slot"));
    }
}
