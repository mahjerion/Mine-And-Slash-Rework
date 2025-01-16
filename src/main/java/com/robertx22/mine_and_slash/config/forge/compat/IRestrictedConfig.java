package com.robertx22.mine_and_slash.config.forge.compat;

import net.minecraftforge.fml.ModList;

public interface IRestrictedConfig<T> {

    public T getDefaultConfig();

    public boolean isRestricted(T obj);

    public T getSelf();

    public static boolean compatModeIsInstalled() {
        return ModList.get().isLoaded("mns_compat");
    }

}
