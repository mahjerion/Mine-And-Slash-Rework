package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.omen;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.OmenModification;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.omen.Omen;
import com.robertx22.mine_and_slash.database.data.omen.OmenData;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.AffixData;
import com.robertx22.mine_and_slash.tags.all.SlotTags;
import net.minecraft.network.chat.MutableComponent;

public class RerollOmenStatsItemMod extends OmenModification {

    public RerollOmenStatsItemMod(String id) {
        super(ItemModificationSers.REROLL_OMEN_STATS, id);
    }

    @Override
    public void modifyOmen(ExileStack stack) {
        stack.get(StackKeys.OMEN).edit(omen -> {
            Omen reg = omen.getOmen();
            GearRarity rar = omen.getRarity();

            int count = omen.aff.size();
            omen.aff.clear();

            // same selection logic as OmenBlueprint#generate, so the difficulty-based
            // magnitude stays deterministic and only the rolled affixes change
            for (int i = 0; i < count; i++) {
                var affix = ExileDB.Affixes()
                        .getFilterWrapped(x -> reg.affix_types.contains(x.type))
                        .of(x -> !x.requirements.tag_requirements.stream().allMatch(t -> t.included.contains(SlotTags.weapon_family.GUID())))
                        .random();

                var adata = new AffixData(affix.type);
                adata.id = affix.GUID();
                adata.rar = rar.GUID();
                adata.p = OmenData.getStatPercent(omen.rarities, omen.slot_req, rar);
                omen.aff.add(adata);
            }
        });
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return RerollOmenStatsItemMod.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Rerolls the Affixes of the Omen"));
    }
}
