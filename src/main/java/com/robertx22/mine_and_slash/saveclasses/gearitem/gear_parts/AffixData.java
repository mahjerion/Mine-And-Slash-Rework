package com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.registry.FilterListWrap;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.mine_and_slash.database.Weighted;
import com.robertx22.mine_and_slash.database.data.MinMax;
import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.IRerollable;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.IStatsContainer;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.ModRange;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRangeInfo;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.saveclasses.item_classes.tooltips.TooltipStatInfo;
import com.robertx22.mine_and_slash.saveclasses.item_classes.tooltips.TooltipStatWithContext;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;

import java.util.*;
import java.util.stream.Collectors;


public class AffixData implements IRerollable, IStatsContainer {


    // perc
    public Integer p = -1;
    public String id;
    // tier
    public String rar = IRarity.COMMON_ID;
    public Affix.AffixSlot ty;


    public GearRarity getRarity() {
        return ExileDB.GearRarities().get(rar);
    }


    public void upgradeRarity() {

        var r = getRarity();

        if (r.hasHigherRarity()) {
            this.rar = r.getHigherRarity().GUID();
        }

        RerollNumbers();
    }

    public void setMaxRarity() {

        var r = getRarity();

        if (r.hasHigherRarity()) {
            this.rar = r.getHigherRarity().GUID();
        }

        RerollNumbers();
    }

    public void downgradeRarity() {

        var r = getRarity();

        Optional<GearRarity> opt = ExileDB.GearRarities().getList().stream().filter(x -> x.getHigherRarity() == r).findAny();

        if (opt.isPresent()) {
            this.rar = opt.get().GUID();
        }
        RerollNumbers();
    }


    public MinMax getMinMax() {
        return getRarity().stat_percents;
    }

    public AffixData(Affix.AffixSlot type) {
        this.ty = type;
    }


    private AffixData() {
    }

    public boolean isEmpty() {
        return p < 0;
    }

    public Affix.AffixSlot getAffixType() {
        return ty;
    }


    public Affix getAffix() {
        return ExileDB.Affixes()
                .get(this.id);
    }


    @Override
    public void RerollNumbers(GearItemData gear) {
        RerollNumbers();
    }

    public void RerollNumbers() {

        var minmax = getMinMax();
        p = minmax.random();

    }

    public void RerollNumbersMax() {

        var minmax = getMinMax();
        p = minmax.max;

    }

    public final Affix BaseAffix() {
        return ExileDB.Affixes()
                .get(id);
    }

    public List<TooltipStatWithContext> getAllStatsWithCtx(int lvl, GearRarity rar) {
        List<TooltipStatWithContext> list = new ArrayList<>();
        this.BaseAffix()
                .getStats()
                .forEach(x -> {
                    ExactStatData exact = x.ToExactStat(p, lvl);
                    TooltipStatInfo confo = new TooltipStatInfo(exact, p, new StatRangeInfo(ModRange.of(getMinMax())));
                    confo.affix_rarity = this.getRarity();
                    list.add(new TooltipStatWithContext(confo, x, (int) lvl));
                });
        return list;
    }

    public boolean isValid() {
        if (!ExileDB.Affixes()
                .isRegistered(this.id)) {
            return false;
        }
        if (this.isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public List<ExactStatData> GetAllStats(ExileStack stack) {
        var gear = stack.get(StackKeys.GEAR).get();
        return GetAllStats(gear.getLevel());

    }

    public List<ExactStatData> GetAllStats(int lvl) {

        if (!isValid()) {
            return Arrays.asList();
        }

        return this.BaseAffix()
                .getStats()
                .stream()
                .map(x -> x.ToExactStat(p, lvl))
                .collect(Collectors.toList());

    }

    public void create(GearItemData gear, Affix suffix) {
        id = suffix.GUID();
        RerollNumbers(gear);
    }


    // todo this needs an entire blueprint part to allow stuff like.. x affixes are more common, etc
    @Override
    public void RerollFully(GearItemData gear) {

        Affix affix = null;
        try {
            FilterListWrap<Affix> list = ExileDB.Affixes()
                    .getFilterWrapped(x -> x.type == getAffixType() && gear.canGetAffix(x));

            if (list.list.isEmpty()) {
                ExileLog.get().warn("Gear Type: " + gear.gtype + " affixtype: " + this.ty.name());
            }

            affix = list.random();

            this.randomizeTier(gear.getRarity());

        } catch (Exception e) {
            ExileLog.get().warn("Gear Type: " + gear.gtype + " affixtype: " + this.ty.name());
            e.printStackTrace();
        }

        this.create(gear, affix);

    }

    public static AffixData rollGuaranteedTagAffix(GearItemData gear, String tagGuid) {

        FilterListWrap<Affix> list = ExileDB.Affixes()
                .getFilterWrapped(x -> (x.type == Affix.AffixSlot.prefix || x.type == Affix.AffixSlot.suffix)
                        && gear.canGetAffix(x)
                        && x.getAllTagReq().contains(tagGuid));

        if (list.list.isEmpty()) {
            return null; // no affix anywhere carries this tag — caller handles fallback
        }

        Affix affix = list.random();

        AffixData data = new AffixData(affix.type); // slot type comes from the affix we found, not a coin flip
        data.randomizeTier(gear.getRarity());
        data.create(gear, affix);

        return data;
    }


    // this is kinda simplified.. but might be fine


    public void randomizeTier(GearRarity rar) {

        // we use special weight so it can be further customized
        var list = ExileDB.GearRarities()
                .getFilterWrapped(x -> !x.is_unique_item && rar.item_tier >= x.item_tier).list.stream()
                .map(x -> new Weighted<GearRarity>(x, x.affix_rarity_weight)).toList();

        this.rar = RandomUtils.weightedRandom(list).obj.GUID();


    }
    public void setMaxPossibleTier(GearRarity gearRarity) {

        var eligible = ExileDB.GearRarities()
                .getFilterWrapped(x -> !x.is_unique_item && gearRarity.item_tier >= x.item_tier)
                .list;

        GearRarity highest = eligible.stream()
                .max(Comparator.comparingInt(x -> x.item_tier))
                .orElse(null);

        if (highest != null) {
            this.rar = highest.GUID();
        }
    }
}
