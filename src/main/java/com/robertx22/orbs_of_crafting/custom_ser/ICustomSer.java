package com.robertx22.orbs_of_crafting.custom_ser;

import com.robertx22.library_of_exile.registry.serialization.ISerializable;

public interface ICustomSer<T> extends ISerializable<T> {

    public CustomSerializer getSerMap();

    public String getSer();
}
