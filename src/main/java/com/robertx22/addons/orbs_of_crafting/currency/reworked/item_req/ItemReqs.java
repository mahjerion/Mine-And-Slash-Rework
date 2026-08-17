package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req;

import com.robertx22.addons.orbs_of_crafting.currency.base.ExileKeyUtil;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_mod.gear.ExtractSocketItemMod;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.custom.*;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear.*;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.item_types.*;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.jewel.JewelHasAffixesReq;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.map.MapHasHigherRarityReq;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.map.MapIsRarityReq;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.MaxUsesKey;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.RarityKeyInfo;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyMap;
import com.robertx22.library_of_exile.registry.helpers.IdKey;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.tags.all.SlotTags;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;

import java.util.Arrays;
import java.util.List;


// todo add a has x rarity affix
public class ItemReqs extends ExileKeyHolder<ItemRequirement> {
    public ItemReqs(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }


    public static class Datas {

        public static MaximumUsesReq.Data MAX_LEVEL_USES = new MaximumUsesReq.Data("level_up", 5);
        public static MaximumUsesReq.Data RANDOM_MYTHIC_AFFIX = new MaximumUsesReq.Data("random_mythic_affix", 1);
        public static MaximumUsesReq.Data MAX_SHARPENING_STONE_USES = new MaximumUsesReq.Data("sharpening_stone", 1);
        public static MaximumUsesReq.Data MAX_RELIEF_USES = new MaximumUsesReq.Data("relief", 5);
        public static MaximumUsesReq.Data MAX_JEWEL_UPGRADE_USES = new MaximumUsesReq.Data("sure_jewel_up", 3);
        public static MaximumUsesReq.Data MAX_OMEN_RARITY_USES = new MaximumUsesReq.Data("codex_rarity_uses", 1);
        // shared across all 14 "Seed" potential-restore currencies (base + Perfected) so applying any
        // one of them blocks every other one from ever being applied to the same item.
        public static MaximumUsesReq.Data SEED_USES = new MaximumUsesReq.Data("seed_uses", 1);

        public static List<MaximumUsesReq.Data> allMaxUses() {
            return Arrays.asList(MAX_RELIEF_USES, MAX_JEWEL_UPGRADE_USES, MAX_LEVEL_USES, RANDOM_MYTHIC_AFFIX,
                    MAX_SHARPENING_STONE_USES, MAX_OMEN_RARITY_USES, SEED_USES);
        }
    }

    public static ItemReqs INSTANCE = new ItemReqs(MMORPG.REGISTER_INFO);

    public ExileKeyMap<ItemRequirement, RarityKeyInfo> IS_RARITY = new ExileKeyMap<ItemRequirement, RarityKeyInfo>(this, "is")
            .ofList(ExileKeyUtil.ofGearRarities())
            .build((id, info) -> new IsRarityReq(id, new IsRarityReq.Data(info.rar)));

    public ExileKeyMap<ItemRequirement, RarityKeyInfo> HAS_AFFIX_OF_RARITY = new ExileKeyMap<ItemRequirement, RarityKeyInfo>(this, "has_affix")
            .ofList(ExileKeyUtil.ofGearRarities())
            .build((id, info) -> new HasAffixOfRarity(id, new HasAffixOfRarity.Data(info.rar)));

    public ExileKeyMap<ItemRequirement, RarityKeyInfo> HAS_AFFIX_OF_RARITY_OR_LOWER = new ExileKeyMap<ItemRequirement, RarityKeyInfo>(this, "has_affix_of_rar_or_lower")
            .ofList(ExileKeyUtil.ofGearRarities())
            .build((id, info) -> new HasAffixUnderRarity(id, new HasAffixUnderRarity.Data(info.rar)));

    public ExileKeyMap<ItemRequirement, MaxUsesKey> MAXIMUM_USES = new ExileKeyMap<ItemRequirement, MaxUsesKey>(this, "max_uses")
            .ofList(Datas.allMaxUses(), x -> new MaxUsesKey(x))
            .build((id, info) -> new MaximumUsesReq(id, info.data));
    public ExileKeyMap<ItemRequirement, RarityKeyInfo> IS_SKILL_GEM_RARITY = new ExileKeyMap<ItemRequirement, RarityKeyInfo>(this, "is_skill_gem_rarity")
            .ofList(ExileKeyUtil.ofGearRarities())
            .build((id, info) -> new IsSkillGemRarityReq(id, new IsSkillGemRarityReq.Data(info.rar)));

    public ExileKeyMap<ItemRequirement, IdKey> IS_GEAR_SLOT = new ExileKeyMap<ItemRequirement, IdKey>(this, "is_gear_slot")
            .ofList(Arrays.asList(
                    new IdKey(SlotTags.weapon_family.GUID()),
                    new IdKey(SlotTags.helmet.GUID()),
                    new IdKey(SlotTags.chest.GUID()),
                    new IdKey(SlotTags.pants.GUID()),
                    new IdKey(SlotTags.boots.GUID()),
                    new IdKey(SlotTags.necklace.GUID()),
                    new IdKey(SlotTags.ring.GUID()),
                    new IdKey(SlotTags.offhand_family.GUID())
            ))
            .build((id, info) -> new IsGearSlotTagReq(id, new IsGearSlotTagReq.Data(info.GUID())));


    public ExileKey<ItemRequirement, KeyInfo> LEVEL_NOT_MAX = ExileKey.ofId(this, "lvl_not_max", x -> new LevelNotMaxReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> LEVEL_AT_LEAST_50 = ExileKey.ofId(this, "lvl_is_50", x -> new LevelAtLeastReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> HAS_AFFIXES = ExileKey.ofId(this, "has_affixes", x -> new MustHaveAffixesReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> HAS_PREFIXES = ExileKey.ofId(this, "has_prefixes", x -> new MustHavePrefixesReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> HAS_SUFFIXES = ExileKey.ofId(this, "has_suffixes", x -> new MustHaveSuffixesReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> HAS_IMPLICIT = ExileKey.ofId(this, "has_implicit", x -> new MustHaveImplicitReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> HAS_OTHER_IMPLICIT = ExileKey.ofId(this, "has_other_implicit", x -> new MustHaveOtherPossibleImplicitReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> HAS_CORRUPTION_AFFIXES = ExileKey.ofId(this, "has_corrupt_affixes", x -> new HasCorruptAffixes(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> IS_NOT_CORRUPTED = ExileKey.ofId(this, "is_not_corrupted", x -> new IsNotCorruptedReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> IS_NOT_MIRRORED = ExileKey.ofId(this, "is_not_mirrored", x -> new IsNotMirroredReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> HAS_INFUSION = ExileKey.ofId(this, "has_infusion", x -> new HasInfusionReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> CAN_ADD_SOCKETS = ExileKey.ofId(this, "can_add_sockets", x -> new CanAddSocketsReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> UNDER_20_QUALITY = ExileKey.ofId(this, "is_under_20_quality", x -> new IsUnderQualityReq(x.GUID(), IsUnderQualityReq.UNDER_20));
    public ExileKey<ItemRequirement, KeyInfo> UNDER_21_QUALITY = ExileKey.ofId(this, "is_under_21_quality", x -> new IsUnderQualityReq(x.GUID(), IsUnderQualityReq.UNDER_21));
    public ExileKey<ItemRequirement, KeyInfo> NOT_CRAFTED = ExileKey.ofId(this, "not_crafted_gear", x -> new IsGearNotCraftedReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> HAS_GEM_SOCKETED = ExileKey.ofId(this, "has_socketed_gem", x -> new HasSocketedReq(x.GUID(), ExtractSocketItemMod.SocketedType.GEM));
    public ExileKey<ItemRequirement, KeyInfo> HAS_RUNE_SOCKETED = ExileKey.ofId(this, "has_socketed_rune", x -> new HasSocketedReq(x.GUID(), ExtractSocketItemMod.SocketedType.RUNE));
    public ExileKey<ItemRequirement, KeyInfo> HAS_HIGHER_RARITY = ExileKey.ofId(this, "has_higher_rar", x -> new HasHigherRarityReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> OMEN_HAS_HIGHER_RARITY = ExileKey.ofId(this, "omen_has_higher_rar", x -> new OmenHasHigherRarityReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> HAS_NOTHING_SOCKETED = ExileKey.ofId(this, "has_nothing_socketed", x -> new HasNothingSocketedReq(x.GUID()));


    // maps
    public ExileKey<ItemRequirement, KeyInfo> MAP_HAS_HIGHER_RARITY = ExileKey.ofId(this, "map_has_higher_rar", x -> new MapHasHigherRarityReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> MAP_IS_MYTHIC = ExileKey.ofId(this, "map_is_mythic", x -> new MapIsRarityReq(x.GUID(), new MapIsRarityReq.Data(IRarity.MYTHIC_ID)));


    // any
    public ExileKey<ItemRequirement, KeyInfo> IS_COMMON_OR_UNCOMMON = ExileKey.ofId(this, "is_common_or_uncommon",
            x -> new SlashIsAnyReq(
                    x.GUID(),
                    new SlashIsAnyReq.Data(Arrays.asList(
                            IS_RARITY.getId(new RarityKeyInfo(IRarity.COMMON_ID)),
                            IS_RARITY.getId(new RarityKeyInfo(IRarity.UNCOMMON))
                    )),
                    "Must be Common Or Uncommon")
    );
    public ExileKey<ItemRequirement, KeyInfo> IS_SKILL_GEM_COMMON_OR_UNCOMMON = ExileKey.ofId(this, "is_skill_gem_common_or_uncommon", x -> new IsSkillGemCommonOrUncommonReq(x.GUID()));

    // nones


    public ExileKey<ItemRequirement, KeyInfo> JEWEL_HAS_AFFIXES = ExileKey.ofId(this, "jewel_has_affixes", x -> new JewelHasAffixesReq(x.GUID()));


    // item types
    public ExileKey<ItemRequirement, KeyInfo> IS_SOUL = ExileKey.ofId(this, "is_soul", x -> new BeSoulReq());
    public ExileKey<ItemRequirement, KeyInfo> IS_JEWEL = ExileKey.ofId(this, "is_jewel", x -> new BeJewelReq());
    public ExileKey<ItemRequirement, KeyInfo> IS_OMEN = ExileKey.ofId(this, "is_omen", x -> new BeOmenReq());
    public ExileKey<ItemRequirement, KeyInfo> IS_GEAR = ExileKey.ofId(this, "is_gear", x -> new BeGearReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> IS_MAP = ExileKey.ofId(this, "is_map", x -> new BeDungeonMapReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> IS_AURA_GEM = ExileKey.ofId(this, "is_aura_gem", x -> new BeAuraGemReq());
    public ExileKey<ItemRequirement, KeyInfo> IS_SUPPORT_GEM = ExileKey.ofId(this, "is_support_gem", x -> new BeSupportGemReq());


    @Override
    public void loadClass() {


    }
}
