package com.robertx22.mine_and_slash.uncommon.effectdatas;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class OnDeathEvent extends EffectEvent {

    public static String ID = "on_death";

    @Override
    public String GUID() {
        return ID;
    }

    public DamageSource dmg;

    public OnDeathEvent(LivingEntity source, LivingEntity target, DamageSource dmg) {
        super(1, source, target);
        this.dmg = dmg;
    }


    @Override
    public String getName() {
        return "On Death Event";
    }

    @Override
    public boolean canWorkOnDeadTarget() {
        return true;
    }

    @Override
    protected void activate() {

    }
}

