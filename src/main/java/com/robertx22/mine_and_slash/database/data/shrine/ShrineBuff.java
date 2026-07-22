package com.robertx22.mine_and_slash.database.data.shrine;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;

// A single datapack-defined buff option that a Shrine (ShrineBlock) can grant. Shrines pick a
// weighted-random ShrineBuff from the whole registry and apply its exile effect to every player in
// range. Datapackers add more options simply by dropping another JSON into this registry's folder;
// the registry itself IS "the list of buffs" the shrine chooses from.
public class ShrineBuff implements JsonExileRegistry<ShrineBuff>, IAutoGson<ShrineBuff> {

    public static ShrineBuff SERIALIZER = new ShrineBuff("ser", "", 1000, 20 * 60);

    public String id = "";

    // GUID of the ExileEffect (see ModEffects / the exile_effect registry) this shrine buff applies.
    public String effect_id = "";

    // weighted-random pick weight relative to the other registered shrine buffs.
    public int weight = 1000;

    // how long the granted buff lasts, in ticks (20 ticks = 1 second).
    public int duration_ticks = 20 * 60;

    public ShrineBuff(String id, String effect_id, int weight, int duration_ticks) {
        this.id = id;
        this.effect_id = effect_id;
        this.weight = weight;
        this.duration_ticks = duration_ticks;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.SHRINE_BUFF;
    }

    @Override
    public Class<ShrineBuff> getClassForSerialization() {
        return ShrineBuff.class;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }
}
