package com.robertx22.mine_and_slash.uncommon.effectdatas;

import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;

import java.util.HashMap;
import java.util.List;

public class DamageConversionLogic {

    public HashMap<Elements, List<Data>> map = new HashMap<>();

    public static class Data {

        public Elements from;
        public Elements to;
        public int percent;

        public Data(Elements from, Elements to, int percent) {
            this.from = from;
            this.to = to;
            this.percent = percent;
        }
    }
}
