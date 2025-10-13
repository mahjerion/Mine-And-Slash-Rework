package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.GearModification;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemModificationSers;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.database.data.MinMax;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarityType;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.SocketData;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.network.chat.MutableComponent;

public class UpgradeRarityItemMod extends GearModification {
    public static enum UpgradeType {
        UPGRADE(Words.UPGRADE_RARITY, Words.AN_AFFIX) {
            @Override
            public GearRarity getNewRarity(GearItemData gear) {
                return gear.getRarity().getHigherRarity();
            }
        },

        RANDOMIZE(Words.RANDOMIZE_RARITY, Words.AFFIXES) {
            @Override
            public GearRarity getNewRarity(GearItemData gear) {
                GearRarity oldRarity = gear.getRarity();
                return ExileDB.GearRarities().getFilterWrapped(x -> {
                    if (x == oldRarity) {
                        return false;
                    }
                    if (gear.lvl < x.min_lvl) {
                        return false;
                    }
                    if (x.type != GearRarityType.NORMAL) {
                        return false;
                    }
                    return true;
                }).random();
            }
        };

        public Words action;
        public Words affixPlural;

        UpgradeType(Words action, Words affixPlural) {
            this.action = action;
            this.affixPlural = affixPlural;
        }

        public abstract GearRarity getNewRarity(GearItemData gear);
    }

    public UpgradeType type;

    public UpgradeRarityItemMod(String id, UpgradeType type) {
        super(ItemModificationSers.UPGRADE_GEAR_RARITY, id);
        this.type = type;
    }

    public static int uniformRescaleInt(int x, MinMax from, MinMax to) {
        return Math.min(to.min + (x - from.min) * (to.max - to.min + 1) / (from.max - from.min), to.max);
    }

    @Override
    public void modifyGear(ExileStack stack, ItemModificationResult r) {
        stack.get(StackKeys.GEAR).edit(gear -> {
            GearRarity oldRarity = gear.getRarity();
            GearRarity newRarity = type.getNewRarity(gear);
            gear.rar = newRarity.GUID();

            // Rescale affix values to upgraded roll range
            MinMax oldRange = oldRarity.stat_percents;
            MinMax newRange = newRarity.stat_percents;

            gear.affixes.getPrefixesAndSuffixes().forEach(affix -> {
                if (affix.p < oldRange.max) {
                    affix.p = uniformRescaleInt(affix.p, new MinMax(0, oldRange.max), new MinMax(0, newRange.max));
                } else {
                    // The affix was already at or above the top of the range, just make sure it stays there
                    affix.p = Math.max(affix.p, newRange.max);
                }

                while (affix.p > affix.getRarity().stat_percents.max) {
                    // New affix value requires rarity upgrade
                    affix.rar = affix.getRarity().getHigherRarity().GUID();
                }
            });

            // Rescale base stats to upgraded roll range
            gear.baseStats.p = uniformRescaleInt(gear.baseStats.p, oldRarity.base_stat_percents, newRarity.base_stat_percents);

            // Add new affix
            for (int affixesToAdd = newRarity.getAffixAmount() - gear.affixes.getNumberOfAffixes(); affixesToAdd > 0; affixesToAdd--) {
                gear.affixes.addOneRandomAffix(gear);
            }

            // Pop out runes/gems and remove sockets if needed
            for (int index = gear.sockets.getSocketedGemsCount() - 1; index >= newRarity.sockets.max; index--) {
                SocketData socket = gear.sockets.getSocketed().get(index);
                r.extraItemsCreated.add(socket.getOriginalItemStack());
                gear.sockets.getSocketed().remove(index);
            }

            for (int index = gear.sockets.getTotalSockets() - 1; index >= newRarity.sockets.max; index--) {
                gear.sockets.removeSocket();
            }
        });
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.GOOD;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return UpgradeRarityItemMod.class;
    }


    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(type.action.locName(), type.affixPlural.locName());
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "%1$s, increasing Numbers and adding %2$s"));
    }

}
