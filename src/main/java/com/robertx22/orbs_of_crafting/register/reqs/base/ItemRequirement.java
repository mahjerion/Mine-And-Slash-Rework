package com.robertx22.orbs_of_crafting.register.reqs.base;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.gear.LevelNotMaxReq;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.registry.register_info.ExileRegistrationInfo;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocDesc;
import com.robertx22.orbs_of_crafting.custom_ser.CustomSerializer;
import com.robertx22.orbs_of_crafting.custom_ser.CustomSerializers;
import com.robertx22.orbs_of_crafting.custom_ser.GsonCustomSer;
import com.robertx22.orbs_of_crafting.main.StackHolder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public abstract class ItemRequirement implements JsonExileRegistry<ItemRequirement>, IAutoLocDesc, GsonCustomSer<ItemRequirement> {

    public static ItemRequirement SERIALIZER = new LevelNotMaxReq("");

    public String serializer = "";

    public String id = "";

    public transient String locname = "";

    public int weight = 1000;


    public ItemRequirement(String serializer, String id) {
        this.serializer = serializer;
        this.id = id;
    }


    public abstract MutableComponent getDescWithParams();


    @Override
    public void addToSerializables(ExileRegistrationInfo info) {
        getSerMap().register(this);
        Database.getRegistry(this.getExileRegistryType()).addSerializable(this, info);
    }

    @Override
    public CustomSerializer getSerMap() {
        return CustomSerializers.ITEM_REQ;
    }

    @Override
    public String getSer() {
        return serializer;
    }


    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.ITEM_REQ;
    }


    @Override
    public String GUID() {
        return id;
    }

    @Override
    public AutoLocGroup locDescGroup() {
        return AutoLocGroup.Currency_Items;
    }

    @Override
    public String locDescLangFileGUID() {
        return SlashRef.MODID + ".item_req." + GUID();
    }

    @Override
    public int Weight() {
        return weight;
    }

    public abstract boolean isValid(Player p, StackHolder obj);


}
