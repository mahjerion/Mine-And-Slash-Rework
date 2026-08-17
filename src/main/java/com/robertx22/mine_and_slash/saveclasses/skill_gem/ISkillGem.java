package com.robertx22.mine_and_slash.saveclasses.skill_gem;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.ModRange;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRangeInfo;
import com.robertx22.mine_and_slash.saveclasses.item_classes.tooltips.TooltipStatInfo;
import com.robertx22.mine_and_slash.saveclasses.item_classes.tooltips.TooltipStatWithContext;
import com.robertx22.mine_and_slash.uncommon.enumclasses.PlayStyle;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface ISkillGem extends IAutoLocName {


    int getRequiredLevel();

    PlayStyle getStyle();

    // spells don't have stat mods, their tooltip goes through Spell.GetTooltipString
    default List<StatMod> getStatMods() {
        return Arrays.asList();
    }

    // mirrors AffixData.getAllStatsWithCtx, the only path that can render a roll range
    default List<TooltipStatWithContext> getAllStatsWithCtx(EntityData en, SkillGemData data) {

        List<TooltipStatWithContext> list = new ArrayList<>();

        int lvl = en.getLevel();
        int perc = data.getStatPercent();
        GearRarity rar = data.getRarity();

        for (StatMod mod : getStatMods()) {
            ExactStatData exact = mod.ToExactStat(perc, lvl);
            TooltipStatInfo info = new TooltipStatInfo(exact, perc, new StatRangeInfo(ModRange.of(rar.stat_percents)));
            info.affix_rarity = rar;
            list.add(new TooltipStatWithContext(info, mod, lvl));
        }

        return list;
    }

}
