package com.robertx22.mine_and_slash.uncommon.effectdatas;

public class DamageInitEvent extends EffectEvent {
    public static String ID = "on_damage_init";

    public DamageEvent dmg;

    public DamageInitEvent(DamageEvent dmg) {
        super(1, dmg.source, dmg.target);
        this.data = dmg.data;
        this.dmg = dmg;
        this.simulation = dmg.simulation;
        this.calcTargetEffects = dmg.calcTargetEffects;
    }

    @Override
    public String getName() {
        return "dmg init";
    }

    @Override
    protected void activate() {

    }

    @Override
    public String GUID() {
        return ID;
    }
}
