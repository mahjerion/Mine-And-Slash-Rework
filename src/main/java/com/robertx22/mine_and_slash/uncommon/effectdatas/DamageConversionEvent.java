package com.robertx22.mine_and_slash.uncommon.effectdatas;

import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

// todo 
public class DamageConversionEvent extends EffectEvent {
    public static String ID = "damage_conversion_event";

    public float number;
    public Elements originalElement;
    DamageEvent dmg;

    public float unconvertedDamagePercent = 100;

    public HashMap<Elements, Float> totals = new HashMap<>();


    public DamageConversionEvent(float num, Elements originalElement, DamageEvent dmg, LivingEntity source, LivingEntity target) {
        super(num, source, target);
        this.number = num;
        this.originalElement = originalElement;
        this.dmg = dmg;
    }

    @Override
    public String getName() {
        return "Damage Conversion Event";
    }

    @Override
    protected void activate() {

        DamageConversionLogic logic = new DamageConversionLogic();


        int total = 0;

        for (Map.Entry<Elements, Float> en : totals.entrySet()) {
            total += en.getValue();
        }

        for (Map.Entry<Elements, Float> en : totals.entrySet()) {
            total += en.getValue();
        }


    }

    @Override
    public String GUID() {
        return ID;
    }
}
