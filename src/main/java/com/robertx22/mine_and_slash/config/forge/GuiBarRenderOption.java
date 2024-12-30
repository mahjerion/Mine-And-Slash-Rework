package com.robertx22.mine_and_slash.config.forge;

import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.config.forge.compat.DamageCompatibilityType;

public enum GuiBarRenderOption {
    DEFAULT, ALWAYS, WHEN_NOT_FULL;

    public GuiBarRenderOption getReal() {
        if (this == DEFAULT) {
            if (CompatConfig.get().DAMAGE_COMPATIBILITY.get() == DamageCompatibilityType.FULL_COMPATIBILITY) {
                return WHEN_NOT_FULL;
            } else {
                return ALWAYS;
            }
        }
        return this;
    }
}
