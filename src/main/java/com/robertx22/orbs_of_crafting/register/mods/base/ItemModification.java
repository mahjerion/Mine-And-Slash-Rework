package com.robertx22.orbs_of_crafting.register.mods.base;

import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.registry.register_info.ExileRegistrationInfo;
import com.robertx22.orbs_of_crafting.custom_ser.CustomSerializer;
import com.robertx22.orbs_of_crafting.custom_ser.CustomSerializers;
import com.robertx22.orbs_of_crafting.custom_ser.GsonCustomSer;
import com.robertx22.orbs_of_crafting.main.OrbDatabase;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.DestroyItemMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

// test if this loads correctly
public abstract class ItemModification implements JsonExileRegistry<ItemModification>, GsonCustomSer<ItemModification>, ITranslated {

    public static ItemModification SERIALIZER = new DestroyItemMod("");

    public String serializer = "";

    public String id = "";

    public transient String locname = "";

    public int weight = 1000;


    public ItemModification(String serializer, String id) {
        this.serializer = serializer;
        this.id = id;
    }

    public enum OutcomeType {
        GOOD(ChatFormatting.GREEN),
        BAD(ChatFormatting.RED),
        NEUTRAL(ChatFormatting.GRAY);

        public ChatFormatting color;

        OutcomeType(ChatFormatting color) {
            this.color = color;
        }
    }

    public abstract OutcomeType getOutcomeType();

    public abstract void applyINTERNAL(StackHolder stack, ItemModificationResult r);


    public void applyMod(Player p, StackHolder stack, ItemModificationResult r) {
        applyINTERNAL(stack, r);
    }

    public abstract MutableComponent getDescWithParams();


    @Override
    public void addToSerializables(ExileRegistrationInfo info) {
        // todo this might be bit bad..
        // i cant disable adding stuff to seriazable if i also do stuff like this like registering custom ser here
        getSerMap().register(this);
        Database.getRegistry(this.getExileRegistryType()).addSerializable(this, info);
    }

    @Override
    public CustomSerializer getSerMap() {
        return CustomSerializers.ITEM_MODIFICATION;
    }

    @Override
    public String getSer() {
        return serializer;
    }


    @Override
    public ExileRegistryType getExileRegistryType() {
        return OrbDatabase.ITEM_MOD;
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
