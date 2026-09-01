package com.robertx22.mine_and_slash.database.data.value_calc;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class ScalingCalc {

    public String stat;
    public LeveledValue multi;

    public Stat getStat() {
        return ExileDB.Stats()
                .get(stat);
    }

    public ScalingCalc() {

    }

    public ScalingCalc(Stat stat, LeveledValue multi) {
        super();
        this.stat = stat.GUID();
        this.multi = multi;
    }

    public LeveledValue getMulti() {
        return multi;
    }

    public int getMultiAsPercent(LivingEntity en, MaxLevelProvider provider) {
        return (int) (multi.getValue(en, provider) * 100);
    }

    public Component GetStatTooltipString(LivingEntity en, MaxLevelProvider provider) {
        return Gui.SPELL_DAMAGE_PROPORTION.locName(getMultiAsPercent(en, provider), getStat().getMutableIconNameFormat());
    }

    public Component GetTargetStatTooltipString(LivingEntity en, MaxLevelProvider provider) {
        return Gui.TARGET_SPELL_DAMAGE_PROPORTION.locName(getMultiAsPercent(en, provider), getStat().getMutableIconNameFormat());
    }

    public List<Component> getTooltipFor(float multi, float value, MutableComponent statname, Elements el) {
        List<Component> list = new ArrayList<>();
        String eleStr = "";

        if (el != null) {
            eleStr = el.format + el.icon;
        }


        if (statname != null) {
            list.add(Component.literal(
                            ChatFormatting.RED + "Scales with " + (int) (multi * 100F) + "% " + eleStr + " ").append(
                            statname)
                    .append(" (" + value + ")"));
        }

        return list;
    }

    public int getCalculatedValue(LivingEntity en, MaxLevelProvider provider) {
        float multi = (getMulti().getValue(en, provider));

        int val = (int) (multi * Load.Unit(en)
                .getUnit()
                .getCalculatedStat(stat)
                .getValue());

        // a monster's flat base damage used to be added here, once per scaling, unmultiplied. it now
        // lives in ValueCalculation.getCalculatedScalingValue, which is the only place that can see
        // the calc's damage effectiveness - see the note there for why that matters.
        return val;

    }
}
