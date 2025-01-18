package com.robertx22.library_of_exile.registry.helpers;

import net.minecraft.data.CachedOutput;

public abstract class ExileDatabaseInit {

    public String modid;

    public ExileDatabaseInit(String modid) {
        this.modid = modid;

        this.registerGatherEvents();
    }

    // just for adding database types
    public abstract void initDatabases();


    // it's fine as long as registers are called before mod is done constructing, but here for ease of use
    // should call at mod constructor,as early as possible, and all calls should be lazy
    public abstract void registerGatherEvents();

    public abstract void runDataGen(CachedOutput cache);


}
