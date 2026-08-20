package com.robertx22.mine_and_slash.maps;

import com.google.common.collect.ImmutableMap;
import com.robertx22.dungeon_realm.item.DungeonItemMapData;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.dungeon_realm.tooltip.MapTooltip;
import com.robertx22.library_of_exile.utils.ItemstackDataSaver;
import com.robertx22.mine_and_slash.aoe_data.database.stats.OffenseStats;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarityType;
import com.robertx22.mine_and_slash.database.data.stats.types.generated.ElementalResist;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.health.Health;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleAutoSalvageRarity;
import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import com.robertx22.mine_and_slash.gui.texts.IgnoreNullList;
import com.robertx22.mine_and_slash.gui.texts.textblocks.*;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.loot.blueprints.MapBlueprint;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.RarityItems;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.ModRange;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRangeInfo;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRequirement;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.TooltipContext;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.SimpleStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.ICommonDataItem;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.uncommon.localization.Itemtips;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.TooltipUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.robertx22.mine_and_slash.gui.texts.ExileTooltips.EMPTY_LINE;

public class MapItemData implements ICommonDataItem<GearRarity> {

    private static MapItemData empty;

    // todo this is in dungeon mod
    // public String uber = "";

    public int lvl = 1;
    public int tier = 0;
    public String rar = IRarity.COMMON_ID;

    public List<MapAffixData> affixes = new ArrayList<MapAffixData>();


    public String uuid = UUID.randomUUID().toString();


    public void setRarityAndRerollNeeded(GearRarity rar) {

        this.rar = rar.GUID();

        // only the affixes that need adding or removing change - upgrading a Rare map with three
        // affixes you liked used to hand back an Epic with four completely different ones.
        MapBlueprint.reconcileAffixes(this, rar);

        // rarity bands are contiguous and an upgrade always moves up a band, so the new roll is
        // already >= the old tier. max() only guards datapacks that define overlapping bands.
        tier = Math.max(tier, getRarity().map_tiers.random());
        uuid = UUID.randomUUID().toString();

    }

    // The highest tier any map can reach. No explicit cap exists - 100 is emergent from the top
    // rarity's band - so derive it, and a datapack that extends the rarity ladder extends the ceiling.
    public static int maxMapTier() {
        return ExileDB.GearRarities().getFilterWrapped(x -> x.type == GearRarityType.NORMAL).list
                .stream()
                .mapToInt(x -> x.map_tiers.max)
                .max()
                .orElse(100);
    }

    // The rarity whose band contains this tier. Must filter to NORMAL - the same predicate
    // GearRarityPart uses for maps - because Unique and Runeword keep the default 0-100 map_tiers
    // and would otherwise match every tier.
    public static GearRarity rarityForTier(int tier) {

        GearRarity best = null;

        for (GearRarity r : ExileDB.GearRarities().getFilterWrapped(x -> x.type == GearRarityType.NORMAL).list) {
            if (r.map_tiers.min > tier) {
                continue;
            }
            if (best == null || r.map_tiers.min > best.map_tiers.min
                    || (r.map_tiers.min == best.map_tiers.min && r.item_tier > best.item_tier)) {
                best = r;
            }
        }

        return best == null ? ExileDB.GearRarities().get(IRarity.COMMON_ID) : best;
    }

    // Tier is the primary axis; rarity is re-derived from it. Everything that reads getRarity()
    // (resist req, xp multi, affix count, omen difficulty, salvage) stays consistent with the tier.
    // Deliberately does not touch uuid - that identifies the active run for the prophecy system.
    public void setTier(int tier) {
        this.tier = MathHelper.clamp(tier, 0, maxMapTier());
        this.rar = rarityForTier(this.tier).GUID();
        MapBlueprint.reconcileAffixes(this, getRarity());
    }


    public MapItemData() {


    }


    public static MapItemData empty() {
        if (empty == null) {
            empty = new MapItemData();
        }
        return empty;

    }


    public StatRequirement getStatReq() {

        StatRequirement req = new StatRequirement();

        for (Elements ele : Elements.getAllSingle()) {
            if (ele != Elements.Physical) {
                var stat = new ElementalResist(ele);

                int number = getRarity().map_resist_req;

                for (MapAffixData affix : this.affixes) {
                    if (affix.getAffix() != null && affix.getAffix().map_resist == ele) {
                        number += affix.getAffix().map_resist_bonus_needed;
                    }
                }

                if (number > 0) {
                    req.base_req.put(stat.GUID(), (float) number);
                }
            }
        }

        return req;
    }

    public List<ExactStatData> getTierStats() {


        List<ExactStatData> stats = new ArrayList<>();

        stats.add(ExactStatData.noScaling((float) (GameBalanceConfig.get().HP_MOB_BONUS_PER_MAP_TIER * tier * 100F), ModType.MORE, Health.getInstance().GUID()));
        stats.add(ExactStatData.noScaling((float) (GameBalanceConfig.get().DMG_MOB_BONUS_PER_MAP_TIER * tier * 100F), ModType.MORE, OffenseStats.TOTAL_DAMAGE.get().GUID()));

        return stats;
    }

    public boolean isEmpty() {
        return this.equals(empty());
    }

    public float getBonusLootMulti() {
        return bonusFormula();

    }

    public float bonusFormula() {
        float tierBonus = tier * 0.02F;
        return (1 + tierBonus);
    }

    public float getExpMulti() {
        return getRarity().map_xp_multi;
    }

    public List<MapAffixData> getAllAffixesThatAffect(AffectedEntities aff) {
        return affixes.stream().filter(x -> x.getAffix() != null && x.getAffix().affected == aff).collect(Collectors.toList());
    }

    public List<Component> getTooltip(ExileStack stack) {

        int lvl = Load.Unit(ClientOnly.getPlayer()).getLevel();

        int min = ServerContainer.get().MIN_LEVEL_MAP_DROPS.get();

        if (lvl < min) {
            lvl = min;
        }

        MapItemData thisMapItemData = this;
        StatRangeInfo tooltipInfo = new StatRangeInfo(ModRange.of(getRarity().stat_percents));
        var tip = new ExileTooltips()
                .accept(new NameBlock(Collections.singletonList(Component.translatable("item.mmorpg.map"))))
                .accept(new RequirementBlock()
                        .setLevelRequirement(lvl)
                        .setStatRequirement(getStatReq()))
                .accept(new RarityBlock(this.getRarity()))
                .accept(new StatBlock() {
                    @Nonnull
                    private final MapItemData mapItemData = thisMapItemData;

                    private final ImmutableMap<AffectedEntities, MutableComponent> map = ImmutableMap.of(
                            AffectedEntities.Mobs, Words.Mob_Affixes.locName(),
                            AffectedEntities.Players, Words.Player_Affixes.locName(),
                            AffectedEntities.All, Words.Affixes_Affecting_All.locName()
                    );

                    @Override
                    public List<? extends Component> getAvailableComponents() {
                        IgnoreNullList<Component> list = new IgnoreNullList<>();
                        Stream.of(AffectedEntities.Mobs, AffectedEntities.Players, AffectedEntities.All).forEachOrdered(x -> getAffectedStatList(list, tooltipInfo, x));
                        list.add(Itemtips.TIER_INFLUENCE.locName().withStyle(ChatFormatting.BLUE));
                        mapItemData.getTierStats().forEach(exactStatData -> list.addAll(exactStatData.GetTooltipString()));
                        return list;
                    }

                    private void getAffectedStatList(IgnoreNullList<Component> list, StatRangeInfo info, AffectedEntities target) {
                        List<MutableComponent> list1 = Optional.of(target)
                                .map(mapItemData::getAllAffixesThatAffect)
                                .filter(x -> !x.isEmpty())
                                .stream()
                                .flatMap(Collection::stream)
                                .map(x -> x.getAffix().getStats(x.p, mapItemData.getLevel()))
                                .flatMap(x -> x.stream()
                                        .map(y -> y.GetTooltipString())
                                        .flatMap(Collection::stream)
                                )
                                .sorted((s1, s2) -> {
                                    //sort long stat
                                    Boolean s1IfLong = s1.getString().contains("\u25C6");
                                    Boolean s2IfLong = s2.getString().contains("\u25C6");
                                    return s1IfLong.compareTo(s2IfLong);
                                })
                                .filter(x -> Objects.nonNull(x) && !x.getString().isBlank())
                                .toList();
                        if (!list1.isEmpty()) {
                            list.add(map.get(target).withStyle(ChatFormatting.BLUE));
                            list.addAll(list1);
                            list.add(EMPTY_LINE);
                        }

                    }
                })
                .accept(new AdditionalBlock(() -> {
                    List<Component> additional = new ArrayList<>();
                    // Load DungeonItemMapData and check for uber/pinnacle status
                    DungeonItemMapData dungeonData = DungeonItemNbt.DUNGEON_MAP.loadFrom(stack.getStack());

                    if (dungeonData != null) {
                        additional.add(MapTooltip.MapLayoutName(dungeonData.dungeon));
                        // mutually exclusive upgrades, so only one indicator ever shows
                        if (dungeonData.pinnacle) {
                            additional.add(MapTooltip.MapHasPinnacle()); // Added Pinnacle Map indicator
                        } else if (dungeonData.uber) {
                            additional.add(MapTooltip.MapHasUber()); // Added Uber Map indicator
                        }
                    }

                    if (!tooltipInfo.shouldShowDescriptions()) {
                        additional.add(Itemtips.Exp.locName(this.getBonusExpAmountInPercent()).withStyle(ChatFormatting.GOLD));
                        additional.add(Itemtips.Loot.locName(this.getBonusLootAmountInPercent()).withStyle(ChatFormatting.GOLD));
                        additional.add(TooltipUtils.tier(this.tier).withStyle(ChatFormatting.GOLD));
                    } else {
                        additional.add(Itemtips.Exp.locName(this.getBonusExpAmountInPercent()).withStyle(ChatFormatting.GOLD));
                        additional.add(Itemtips.Loot.locName(this.getBonusLootAmountInPercent()).withStyle(ChatFormatting.GOLD));
                        additional.add(TooltipUtils.tier(this.tier).withStyle(ChatFormatting.GOLD));
                        additional.add(Component.literal("[" + Itemtips.SOUL_TIER_TIP.locName().getString() + "]").withStyle(ChatFormatting.BLUE));
                    }
                    return additional;
                }))
                //handle possibleRarities
                .accept(WorksOnBlock.possibleDrops(ExileDB.GearRarities().getFilterWrapped(
                        x -> this.tier >= ExileDB.GearRarities().get(x.min_map_rarity_to_drop).map_tiers.min
                ).list).notDraggable());


        tip.accept(new OperationTipBlock().setAlt());

        return tip.release();

    }



    @Override
    public void BuildTooltip(TooltipContext ctx) {
        if (ctx.data != null) {
            ctx.tooltip.clear();
            ctx.tooltip.addAll(getTooltip(ExileStack.of(ctx.stack)));

        }
    }

    private int getBonusLootAmountInPercent() {
        return (int) ((this.getBonusLootMulti() - 1) * 100);
    }

    private int getBonusExpAmountInPercent() {
        int exp = (int) ((this.getExpMulti() - 1) * 100);
        return exp;
    }

    public List<StatContext> getStatAndContext(LivingEntity en) {
        List<ExactStatData> stats = new ArrayList<>();
        try {
            for (MapAffixData affix : this.getAllAffixesThatAffect(AffectedEntities.of(en))) {
                stats.addAll(affix.getAffix().getStats(affix.p, getLevel()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList(new SimpleStatCtx(StatContext.StatCtxType.MOB_AFFIX, stats));
    }

    @Override
    public String getRarityId() {
        return rar;
    }

    public GearRarity getRarity() {
        return ExileDB.GearRarities().get(rar);
    }


    public int getTier() {
        return this.tier;
    }


    public int getLevel() {
        return lvl;
    }


    @Override
    public ItemstackDataSaver<? extends ICommonDataItem> getStackSaver() {
        return StackSaving.MAP;
    }

    @Override
    public void saveToStack(ItemStack stack) {
        StackSaving.MAP.saveTo(stack, this);
    }

    @Override
    public List<ItemStack> getSalvageResult(ExileStack stack) {
        if (!isSalvagable(stack)) {
            return Arrays.asList();
        }
        int amount = 1;

        GearRarity mapRarity = getRarity();

        // Find the rarity to use for the stone, capping at tier 2 (Rare)
        // Find the rarity with item_tier == 2 (Rare)
        Optional<GearRarity> rareRarityOpt = ExileDB.GearRarities()
                .getList()
                .stream()
                .filter(r -> r.item_tier == 2)
                .findFirst();

        GearRarity stoneRarity;
        if (mapRarity.item_tier < 2) {
            stoneRarity = mapRarity;
        } else {
            // If map is Rare or above, use Rare
            stoneRarity = rareRarityOpt.orElse(mapRarity); // fallback to mapRarity if not found
        }

        return Arrays.asList(new ItemStack(
                RarityItems.RARITY_STONE.getOrDefault(stoneRarity.GUID(), RarityItems.RARITY_STONE.get(IRarity.COMMON_ID)).get(),
                amount
        ));
    }

    @Override
    public ToggleAutoSalvageRarity.SalvageType getSalvageType() {
        return ToggleAutoSalvageRarity.SalvageType.MAP;
    }

    @Override
    public String getMapLayoutId(ExileStack stack) {
        DungeonItemMapData d = DungeonItemNbt.DUNGEON_MAP.loadFrom(stack.getStack());

        if (d == null || d.dungeon == null || d.dungeon.isEmpty()) {
            // maps predating fixed layouts pick one at the map device, so there is nothing to filter on
            return null;
        }
        // an upgraded map cost something, never scrap it over its layout
        if (d.uber || d.pinnacle) {
            return null;
        }
        return d.dungeon;
    }
}