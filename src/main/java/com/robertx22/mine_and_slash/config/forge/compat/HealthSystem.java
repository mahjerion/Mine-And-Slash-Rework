package com.robertx22.mine_and_slash.config.forge.compat;

public enum HealthSystem {
    IMAGINARY_MINE_AND_SLASH_HEALTH, VANILLA_HEALTH;

    public boolean addBonusHealthFromVanillaHearts() {
        return this == IMAGINARY_MINE_AND_SLASH_HEALTH;
    }

    public boolean usesVanillaHearts() {
        return this == VANILLA_HEALTH;
    }
}
