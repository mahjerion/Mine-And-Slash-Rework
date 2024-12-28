package com.robertx22.addons.orbs_of_crafting.currency.reworked.addon;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.loot.req.DropRequirement;

import java.util.function.Consumer;

public class ExtendedOrb implements JsonExileRegistry<ExtendedOrb>, IAutoGson<ExtendedOrb> {
    public static ExtendedOrb SERIALIZER = new ExtendedOrb();

    public String id = "";

    public DropRequirement drop_req = DropRequirement.Builder.of().build();

    public ExtendedOrb edit(Consumer<ExtendedOrb> c) {
        c.accept(this);
        return this;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.ORB_EXTEND;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return 1;
    }

    @Override
    public Class<ExtendedOrb> getClassForSerialization() {
        return ExtendedOrb.class;
    }
}
