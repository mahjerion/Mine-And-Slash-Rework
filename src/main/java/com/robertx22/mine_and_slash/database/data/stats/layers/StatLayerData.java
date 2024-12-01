package com.robertx22.mine_and_slash.database.data.stats.layers;

import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;

import java.util.HashMap;
import java.util.Map;


// example: you have 200% fire resist, enemy has 10% fire pene, you end up with 190% fire res which means enemy doesnt do any extra dmg to you
public class StatLayerData {

    public EffectSides side = EffectSides.Source;
    private String layer = "";
    public String numberID = "";
    private float number = 0;

    public Conversion conversion = null;
    public AdditionalConversion additionalConversion = null;

    public static class Conversion {

        public HashMap<Elements, Float> totals = new HashMap<>();

        boolean normalized = false;

        public void add(Elements ele, Float n) {
            if (!totals.containsKey(ele)) {
                totals.put(ele, 0f);
            }
            totals.put(ele, totals.get(ele) + n);
        }

        public void normalizeNumbersToCapTo100() {
            normalized = true;

            float total = 0;
            for (Float v : totals.values()) {
                total += v;
            }
            if (total > 100) {
                float multi = 100F / total;
                for (Map.Entry<Elements, Float> en : new HashMap<>(totals).entrySet()) {
                    totals.put(en.getKey(), totals.get(en.getKey()) * multi);
                }
            }
        }
    }

    public static class AdditionalConversion {

        //public Elements from;
        public Elements to;
        public int percent;

        public AdditionalConversion(Elements to, int percent) {
            this.to = to;
            this.percent = percent;
        }
    }
    //private Set<String> statsThatModifiedThis = new HashSet<>();

    public StatLayerData(String layer, String numberID, float number, EffectSides side) {
        this.layer = layer;
        this.numberID = numberID;
        this.number = number;
        this.side = side;
    }

    public void convertAdditional(Elements to, int perc) {
        this.additionalConversion = new AdditionalConversion(to, perc);
    }

    public void convertDamage(Elements to, int perc) {
        if (conversion == null) {
            conversion = new Conversion();
        }
        this.conversion.add(to, (float) perc);
    }

    public StatLayer getLayer() {
        return ExileDB.StatLayers().get(layer);
    }

    public void add(float num) {
        this.number += num;
    }

    public void multiply(float multi) {
        this.number *= multi;
    }

    public void reduce(float num) {
        this.number -= num;
    }

    public float getNumber() {
        var lay = getLayer();
        float num = number; // MathHelper.clamp(number, lay.min_multi, lay.max_multi);
        return num;
    }

    public float getMultiplier() {
        var lay = getLayer();

        float num = number;
        float multi = 1 + (num / 100F);
        multi = MathHelper.clamp(multi, lay.min_multi, lay.max_multi);
        return multi;

    }
}
