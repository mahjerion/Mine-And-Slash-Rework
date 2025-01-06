package com.robertx22.mine_and_slash.config.forge;

import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;

public enum GuiBarRenderOption {
    DEFAULT, ALWAYS, WHEN_NOT_FULL;

    public GuiBarRenderOption getReal() {
        if (this == DEFAULT) {
            if (!CompatConfig.get().damageSystem().overridesDamage) {
                return WHEN_NOT_FULL;
            } else {
                return ALWAYS;
            }
        }
        return this;
    }
}
