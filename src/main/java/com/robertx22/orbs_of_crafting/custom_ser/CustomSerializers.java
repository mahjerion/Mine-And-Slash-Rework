package com.robertx22.orbs_of_crafting.custom_ser;

import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;

public class CustomSerializers {

    public static CustomSerializer<ItemModification> ITEM_MODIFICATION = new CustomSerializer<>();
    public static CustomSerializer<ItemRequirement> ITEM_REQ = new CustomSerializer<>();

}
