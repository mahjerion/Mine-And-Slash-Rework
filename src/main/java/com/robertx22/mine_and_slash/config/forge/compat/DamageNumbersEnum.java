package com.robertx22.mine_and_slash.config.forge.compat;

public enum DamageNumbersEnum {
    DEFAULT, ON, OFF;

    public boolean getReal() {
        if (this == DEFAULT) {
            return CompatConfig.get().damageSystem().overridesDamage;
        }
        return this == ON;
    }
}
