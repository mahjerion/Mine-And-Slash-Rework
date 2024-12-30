package com.robertx22.mine_and_slash.config.forge.compat;

public enum DamageCompatibilityType {
    FULL_DAMAGE_OVERRIDE(true),
    FULL_COMPATIBILITY(false);

    public boolean overridesDamage;

    DamageCompatibilityType(boolean overridesDamage) {
        this.overridesDamage = overridesDamage;
    }
}
