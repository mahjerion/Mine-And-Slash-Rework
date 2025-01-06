package com.robertx22.mine_and_slash.config.forge.compat;

import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.LivingEntity;

public enum HealthSystem {
    IMAGINARY_MINE_AND_SLASH_HEALTH, VANILLA_HEALTH;

    public boolean addBonusHealthFromVanillaHearts() {
        return this == IMAGINARY_MINE_AND_SLASH_HEALTH;
    }

    public boolean usesVanillaHearts() {
        return this == VANILLA_HEALTH;
    }

    public float getOriginalMaxHealth(LivingEntity en) {
        if (usesVanillaHearts()) {
            return Load.Unit(en).heartsWithoutMnsHealth;
        }
        return en.getMaxHealth();
    }

 
}
