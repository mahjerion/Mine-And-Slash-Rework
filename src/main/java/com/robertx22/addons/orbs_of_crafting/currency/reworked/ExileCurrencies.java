package com.robertx22.addons.orbs_of_crafting.currency.reworked;

import com.robertx22.addons.orbs_of_crafting.currency.base.CodeCurrency;
import com.robertx22.addons.orbs_of_crafting.currency.base.ExileCurrencyItem;
import com.robertx22.addons.orbs_of_crafting.currency.base.ExileKeyUtil;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.ItemMods;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqs;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.MaxUsesKey;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.RarityKeyInfo;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.SkillItemTierKey;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.registry.helpers.*;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.RarityItems;
import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.mine_and_slash.tags.all.SlotTags;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.orbs_of_crafting.misc.ShapedRecipeUTIL;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import com.robertx22.orbs_of_crafting.register.Modifications;
import com.robertx22.orbs_of_crafting.register.Requirements;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Map;


public class ExileCurrencies extends ExileKeyHolder<ExileCurrency> {

    public ExileCurrencies(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public static ExileCurrencies INSTANCE = (ExileCurrencies) new ExileCurrencies(MMORPG.REGISTER_INFO)
            // todo is this a good way to check? I'm thinking note 1 layer of dep
            .itemIds(new ItemIdProvider(x -> SlashRef.id("currency/" + x)))
            .createItems(new ItemCreator<ExileCurrency>(x -> new ExileCurrencyItem(x.get())), x -> Def.item(x.itemID(), x.item()))
            .dependsOn(() -> ItemMods.INSTANCE)
            .dependsOn(() -> ItemReqs.INSTANCE);


    public HarvestCurrencies HARVEST = new HarvestCurrencies(this);
    public JewelCurrencies JEWEL = new JewelCurrencies(this);


    public ExileKeyMap<ExileCurrency, NamedKey> FORCE_SOUL_TAGS = new ExileKeyMap<ExileCurrency, NamedKey>(this, "force_soul_tag")
            .ofList(Arrays.asList(
                    new NamedKey(SlotTags.armor_stat.GUID(), "Armor"),
                    new NamedKey(SlotTags.magic_shield_stat.GUID(), "Magic Shield"),
                    new NamedKey(SlotTags.dodge_stat.GUID(), "Dodge")
            ))
            .build((id, info) -> {
                return ExileCurrency.Builder.of(id, info.name + "-Gear Soul Modifier", ItemReqs.INSTANCE.IS_SOUL)
                        .rarity(IRarity.UNCOMMON)
                        .addRequirement(Requirements.INSTANCE.IS_SINGLE_ITEM)
                        .addAlwaysUseModification(ItemMods.INSTANCE.FORCE_SOUL_TAG.get(info))
                        .potentialCost(0)
                        .weight(0)
                        .buildCurrency(this);
            });

    public ExileKeyMap<ExileCurrency, SkillItemTierKey> SHARPEN_STONE_QUALITY = new ExileKeyMap<ExileCurrency, SkillItemTierKey>(this, "sharpening_stone")
            .ofList(ExileKeyUtil.ofSkillItemTiers())
            .build((id, info) -> {
                return ExileCurrency.Builder.of(id, info.tier.word + " Sharpening Stone", ItemReqs.INSTANCE.IS_GEAR)
                        .rarity(info.tier.rar)
                        .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
                        .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
                        .addAlwaysUseModification(ItemMods.INSTANCE.SHARPEN_STONE_QUALITY.get(info))
                        .edit(MaxUsesKey.ofUses(ItemReqs.Datas.MAX_SHARPENING_STONE_USES.toKey()))
                        .potentialCost(0)
                        .weight(0)
                        .buildCurrency(this);
            });


    public ExileKey<ExileCurrency, IdKey> CORRUPT_GEAR = ExileCurrency.Builder.of("chaos_orb", "Orb of Chaos", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.UNIQUE_ID)
            .addModification(ItemMods.INSTANCE.CORRUPT_GEAR, 75)
            .addModification(Modifications.INSTANCE.DESTROY_ITEM, 25)
            .potentialCost(0)
            .weight(1000)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> MIRROR = ExileCurrency.Builder.of("mirror", "Orb of Reflection", ItemReqs.INSTANCE.IS_GEAR, ItemReqs.INSTANCE.IS_JEWEL)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.MYTHIC_ID)
            .addAlwaysUseModification(ItemMods.INSTANCE.MIRROR)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.MIRROR)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> LEVEL_GEAR = ExileCurrency.Builder.of("level_up_orb", "Orb of Infinity", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.UNCOMMON)
            .addRequirement(ItemReqs.INSTANCE.LEVEL_NOT_MAX)
            .addModification(ItemMods.INSTANCE.ADD_GEAR_LEVEL, 1)
            .edit(MaxUsesKey.ofUses(ItemReqs.Datas.MAX_LEVEL_USES.toKey()))
            .potentialCost(5)
            .weight(1000)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> ADD_SOCKET = ExileCurrency.Builder.of("socket_adder", "Orb of Digging", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.CAN_ADD_SOCKETS)
            .rarity(IRarity.RARE_ID)
            .addModification(ItemMods.INSTANCE.ADD_SOCKET, 50)
            .addModification(Modifications.INSTANCE.DO_NOTHING, 50)
            .potentialCost(10)
            .weight(1000)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> UNIQUE_STAT_REROLL = ExileCurrency.Builder.of("unique_reroll", "Orb of Imperfection", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.RARE_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_RARITY.map.get(new RarityKeyInfo(IRarity.UNIQUE_ID)))
            .addModification(ItemMods.INSTANCE.ADD_5_PERCENT_UNIQUE_STATS, 60)
            .addModification(ItemMods.INSTANCE.REDUCE_5_PERCENT_UNIQUE_STATS, 40)
            .potentialCost(10)
            .weight(CodeCurrency.Weights.UBER)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_RANDOM_AFFIX = ExileCurrency.Builder.of("affix_common_reroll", "Orb of New Beginnings", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.RARE_ID)
            .addRequirement(ItemReqs.INSTANCE.HAS_AFFIXES)
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_RANDOM_AFFIX)
            .potentialCost(10)
            .weight(CodeCurrency.Weights.COMMON)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_ALL_AFFIXES = ExileCurrency.Builder.of("affix_reroll_all", "Orb of Rebirth", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.RARE_ID)
            .addRequirement(ItemReqs.INSTANCE.HAS_AFFIXES)
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_ALL_AFFIXES)
            .potentialCost(10)
            .weight(CodeCurrency.Weights.COMMON)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_ALL_PREFIXES = ExileCurrency.Builder.of("prefix_reroll_all", "Orb of Sinistral Rebirth", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.EPIC_ID)
            .addRequirement(ItemReqs.INSTANCE.HAS_PREFIXES)
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_ALL_PREFIXES)
            .potentialCost(20)
            .weight(CodeCurrency.Weights.RARE)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_ALL_SUFFIXES = ExileCurrency.Builder.of("suffix_reroll_all", "Orb of Dextral Rebirth", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.EPIC_ID)
            .addRequirement(ItemReqs.INSTANCE.HAS_SUFFIXES)
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_ALL_SUFFIXES)
            .potentialCost(20)
            .weight(CodeCurrency.Weights.RARE)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_RANDOM_AFFIX_TO_MYTHIC = ExileCurrency.Builder.of("affix_random_mythic_reroll", "Orb of Divine Benevolence", ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.MYTHIC_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.HAS_AFFIXES)
            .addRequirement(ItemReqs.INSTANCE.NOT_CRAFTED)
            .addRequirement(ItemReqs.INSTANCE.IS_RARITY.get(new RarityKeyInfo(IRarity.MYTHIC_ID)))
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_RANDOM_AFFIX_INTO_MYTHIC)
            .edit(MaxUsesKey.ofUses(ItemReqs.Datas.RANDOM_MYTHIC_AFFIX.toKey()))
            .potentialCost(25)
            .weight(CodeCurrency.Weights.MEGA_UBER)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> UPGRADE_OR_DOWNGRADE_RANDOM_AFFIX = ExileCurrency.Builder.of("affix_tier_up_down", "Orb of Imbalance", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.RARE_ID)
            .addRequirement(ItemReqs.INSTANCE.HAS_AFFIXES)
            .addModification(ItemMods.INSTANCE.UPGRADE_RANDOM_AFFIX, 60)
            .addModification(ItemMods.INSTANCE.DOWNGRADE_RANDOM_AFFIX, 40)
            .potentialCost(5)
            .weight(CodeCurrency.Weights.COMMON)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> UPGRADE_QUALITY = ExileCurrency.Builder.of("orb_of_quality", "Orb of Quality", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.UNDER_20_QUALITY)
            .rarity(IRarity.UNCOMMON)
            .addAlwaysUseModification(ItemMods.INSTANCE.ADD_GEAR_QUALITY)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.COMMON)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_INFUSION = ExileCurrency.Builder.of("enchant_reroll", "Orb of Second Guessing", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.HAS_INFUSION)
            .rarity(IRarity.UNCOMMON)
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_INFUSION)
            .potentialCost(1)
            .weight(CodeCurrency.Weights.RARE)
            .build(this);

/*
    public ExileKey<ExileCurrency, IdKey> MAP_RARITY_UPGRADE = ExileCurrency.Builder.of("map_rarity_upgrade", "Map Rarity Upgrade Orb", WorksOnBlock.ItemType.MAP)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.MAP_HAS_HIGHER_RARITY)
            .rarity(IRarity.EPIC_ID)
            .addAlwaysUseModification(ItemMods.INSTANCE.UPGRADE_MAP_RARITY)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.COMMON)
            .build(this);

 */


    public ExileKey<ExileCurrency, IdKey> UPGRADE_COMMON_AFFIX = ExileCurrency.Builder.of("upgrade_common_affix", "Orb of Fledgling's Reprieve", ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.RARE_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.HAS_AFFIX_OF_RARITY.get(new RarityKeyInfo(IRarity.COMMON_ID)))
            .addAlwaysUseModification(ItemMods.INSTANCE.UPGRADE_SPECIFIC_AFFIX_RARITY.get(new RarityKeyInfo(IRarity.COMMON_ID)))
            .potentialCost(3)
            .weight(CodeCurrency.Weights.COMMON)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_AFFIX_NUMBERS = ExileCurrency.Builder.of("affix_number_reroll", "Orb of Ciphers", ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.RARE_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.HAS_AFFIXES)
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_AFFIX_NUMBERS)
            .potentialCost(5)
            .weight(CodeCurrency.Weights.COMMON)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_PREFIX_NUMBERS = ExileCurrency.Builder.of("prefix_number_reroll", "Orb of Western Ciphers", ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.EPIC_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.HAS_PREFIXES)
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_PREFIX_NUMBERS)
            .potentialCost(10)
            .weight(CodeCurrency.Weights.RARE)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_SUFFIX_NUMBERS = ExileCurrency.Builder.of("suffix_number_reroll", "Orb of Eastern Ciphers", ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.EPIC_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.HAS_SUFFIXES)
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_SUFFIX_NUMBERS)
            .potentialCost(10)
            .weight(CodeCurrency.Weights.RARE)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_IMPLICIT_NUMBERS = ExileCurrency.Builder.of("implicit_number_reroll", "Orb of Genesis", ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.LEGENDARY_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.HAS_IMPLICIT)
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_IMPLICIT_NUMBERS)
            .potentialCost(10)
            .weight(CodeCurrency.Weights.RARE)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_IMPLICIT_AFFIX = ExileCurrency.Builder.of("implicit_affix_reroll", "Orb of Revelation", ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.MYTHIC_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.HAS_OTHER_IMPLICIT)
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_IMPLICIT_AFFIX)
            .potentialCost(15)
            .weight(CodeCurrency.Weights.UBER)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> UPGRADE_CORRUPTION_AFFIX = ExileCurrency.Builder.of("up_corrupt_affix", "Orb of Foolish Risk",
                    ItemReqs.INSTANCE.IS_GEAR, ItemReqs.INSTANCE.IS_JEWEL)
            .rarity(IRarity.EPIC_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .addRequirement(ItemReqs.INSTANCE.HAS_CORRUPTION_AFFIXES)
            .addModification(ItemMods.INSTANCE.UPGRADE_CORRUPTION_AFFIX_RARITY, 90)
            .addModification(Modifications.INSTANCE.DESTROY_ITEM, 10)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.COMMON)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> EXTRACT_GEM = ExileCurrency.Builder.of("extract_gem", "Gem Extractor",
                    ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.UNCOMMON)
            .addRequirement(ItemReqs.INSTANCE.HAS_GEM_SOCKETED)
            .addAlwaysUseModification(ItemMods.INSTANCE.EXTRACT_GEM)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.COMMON)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> EXTRACT_RUNE = ExileCurrency.Builder.of("extract_rune", "Rune Extractor",
                    ItemReqs.INSTANCE.IS_GEAR)
            .rarity(IRarity.UNCOMMON)
            .addRequirement(ItemReqs.INSTANCE.HAS_RUNE_SOCKETED)
            .addAlwaysUseModification(ItemMods.INSTANCE.EXTRACT_RUNE)
            .potentialCost(1)
            .weight(CodeCurrency.Weights.COMMON)
            .build(this);


    public ExileKey<ExileCurrency, IdKey> EASY_ONE_TIME_UPGRADE = ExileCurrency.Builder.of("orb_of_relief", "Orb of Relief", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.EPIC_ID)
            .addRequirement(ItemReqs.INSTANCE.HAS_AFFIXES)
            .addRequirement(ItemReqs.INSTANCE.HAS_AFFIX_OF_RARITY_OR_LOWER.get(new RarityKeyInfo(IRarity.EPIC_ID)))
            .addAlwaysUseModification(ItemMods.INSTANCE.UPGRADE_LOWEST_AFFIX)
            .potentialCost(1)
            .edit(MaxUsesKey.ofUses(ItemReqs.Datas.MAX_RELIEF_USES.toKey()))
            .weight(CodeCurrency.Weights.RARE)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> UPGRADE_GEAR_RARITY = ExileCurrency.Builder.of("gear_rarity_upgrade", "Orb of Ascension", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.MYTHIC_ID)
            .addRequirement(ItemReqs.INSTANCE.HAS_HIGHER_RARITY)
            .addAlwaysUseModification(ItemMods.INSTANCE.UPGRADE_GEAR_RARITY)
            .potentialCost(35)
            .weight(CodeCurrency.Weights.UBER)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> RANDOMIZE_GEAR_RARITY = ExileCurrency.Builder.of("gear_rarity_random", "Orb of Colors", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.UNCOMMON)
            .addRequirement(ItemReqs.INSTANCE.IS_RARITY.get(new RarityKeyInfo(IRarity.COMMON_ID)))
            .addAlwaysUseModification(ItemMods.INSTANCE.RANDOMIZE_GEAR_RARITY)
            .potentialCost(1)
            .weight(CodeCurrency.Weights.COMMON)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> COMMON_TO_RUNED = ExileCurrency.Builder.of("common_to_runed", "Orb of Runic Invocation", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.RUNEWORD_ID)
            .addRequirement(ItemReqs.INSTANCE.IS_RARITY.get(new RarityKeyInfo(IRarity.COMMON_ID)))
            .addModification(ItemMods.INSTANCE.COMMON_TO_RUNED, 75)
            .addModification(Modifications.INSTANCE.DESTROY_ITEM, 25)
            .potentialCost(40)
            .weight(CodeCurrency.Weights.RARE)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> REROLL_BASE_NUMBERS = ExileCurrency.Builder.of("base_number_reroll", "Orb of Introspection", ItemReqs.INSTANCE.IS_GEAR)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_CORRUPTED)
            .addRequirement(ItemReqs.INSTANCE.IS_NOT_MIRRORED)
            .rarity(IRarity.LEGENDARY_ID)
            .addAlwaysUseModification(ItemMods.INSTANCE.REROLL_BASE_NUMBERS)
            .potentialCost(15)
            .weight(CodeCurrency.Weights.UBER)
            .build(this);

    public ExileKey<ExileCurrency, IdKey> UPGRADE_MAP_RARITY = ExileCurrency.Builder.of("map_rarity_upgrade", "Orb of Map Rarity", ItemReqs.INSTANCE.IS_MAP)
            .rarity(IRarity.EPIC_ID)
            .addAlwaysUseModification(ItemMods.INSTANCE.UPGRADE_MAP_RARITY)
            .potentialCost(0)
            .weight(CodeCurrency.Weights.RARE)
            .build(this);


    @Override
    public void loadClass() {


        UPGRADE_MAP_RARITY.addRecipe(LibDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 5)
                    .define('X', RarityItems.RARITY_STONE.get(IRarity.EPIC_ID).get())
                    .define('Y', Items.DIAMOND)
                    .pattern("XXX")
                    .pattern("XYX")
                    .pattern("XXX");
        });

        EXTRACT_GEM.addRecipe(LibDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 3)
                    .define('X', Items.GOLD_INGOT)
                    .define('Y', Items.STICK)
                    .pattern("XXX")
                    .pattern("XXX")
                    .pattern(" Y ");
        });

        EXTRACT_RUNE.addRecipe(LibDatabase.CURRENCY, x -> {
            return ShapedRecipeUTIL.of(x.getItem(), 3)
                    .define('X', Items.DIAMOND)
                    .define('Y', Items.STICK)
                    .pattern("XXX")
                    .pattern("XXX")
                    .pattern(" Y ");
        });


        for (Map.Entry<NamedKey, ExileKey<ExileCurrency, NamedKey>> en : FORCE_SOUL_TAGS.map.entrySet()) {

            Item item = Items.COPPER_INGOT;
            if (en.getKey().GUID().equals(SlotTags.magic_shield_stat.GUID())) {
                item = Items.PAPER;
            }
            if (en.getKey().GUID().equals(SlotTags.dodge_stat.GUID())) {
                item = Items.LEATHER;
            }
            Item finalItem = item;
            en.getValue().addRecipe(LibDatabase.CURRENCY, x -> {
                return ShapedRecipeUTIL.of(x.getItem(), 1)
                        .define('X', finalItem)
                        .define('Y', Items.IRON_INGOT)
                        .pattern("X")
                        .pattern("Y");
            });
        }


    }
}
