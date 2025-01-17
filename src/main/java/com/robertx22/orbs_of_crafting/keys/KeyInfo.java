package com.robertx22.orbs_of_crafting.keys;

import com.robertx22.library_of_exile.util.AutoHashClass;

public abstract class KeyInfo extends AutoHashClass {


    @Override
    public int hashCode() {
        return GUID().hashCode();
    }
}
