package com.robertx22.mine_and_slash.config.forge.compat;

public enum DamageNumbersEnum {
    DEFAULT, ON, OFF;

    public boolean getReal() {
        if (this == DEFAULT) {
            return CompatConfig.get().DAMAGE_COMPATIBILITY().overridesDamage;
        }
        return this == ON;
    }
}
