package com.robertx22.mine_and_slash.config.forge.compat;

public enum DamageCompatibilityType {
    DAMAGE_OVERRIDE(true),
    DAMAGE_BONUS(false);

    public boolean overridesDamage;

    DamageCompatibilityType(boolean overridesDamage) {
        this.overridesDamage = overridesDamage;
    }

  
}
