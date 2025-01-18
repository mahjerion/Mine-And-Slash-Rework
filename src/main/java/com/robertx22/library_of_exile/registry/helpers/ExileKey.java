package com.robertx22.library_of_exile.registry.helpers;

import com.google.common.base.Preconditions;
import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.recipe.RecipeGenerator;
import com.robertx22.library_of_exile.registry.*;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ExileKey<T extends ExileRegistry<T>, Info extends KeyInfo> implements IGUID {

    public Info info;
    private BiFunction<String, Info, T> sup;
    private T obj;
    ExileKeyHolder holder;
    String id;
    RegObj<Item> item = null;
    ModRequiredRegisterInfo modRegisterInfo;


    private boolean cancelItemCreation = false;


    public ExileKey(ExileKeyHolder holder, Info info, BiFunction<String, Info, T> sup, String id) {
        this.holder = holder;
        this.info = info;
        this.modRegisterInfo = holder.modRegisterInfo;
        this.sup = sup;
        this.id = id;

        holder.all.put(GUID(), this);
    }

    public T get() {
        Preconditions.checkNotNull(obj);
        return obj;
    }

    public Item getItem() {
        if (item == null) {
            throw new RuntimeException("No item for registry key: " + id);
        }
        return item.get();
    }


    public ExileKey<T, Info> addRecipe(ExileRegistryType type, Function<ExileKey<T, Info>, ShapedRecipeBuilder> b) {
        RecipeGenerator.addRecipe(modRegisterInfo.modid, () -> {
            return b.apply(this);
        });
        return this;
    }

    public ExileKey<T, Info> doNotCreateItem() {
        this.cancelItemCreation = true;
        return this;
    }

    public boolean isCancelItemCreation() {
        return cancelItemCreation;
    }


    @Override
    public String GUID() {
        return id;
    }

    public static <T extends ExileRegistry<T>, Info extends KeyInfo> ExileKey<T, Info> ofId(ExileKeyHolder holder, String id, Function<Info, T> sup) {
        return new ExileKey<>(holder, (Info) new IdKey(id), (s, info) -> sup.apply(info), id);
    }

    public void register() {
        obj = sup.apply(id, info);

        ExileEvents.EXILE_REGISTRY_GATHER.register(new EventConsumer<ExileRegistryEvent>() {
            @Override
            public void accept(ExileRegistryEvent e) {
                if (e.type == obj.getExileRegistryType()) {
                    if (obj instanceof JsonExileRegistry<?> j) {
                        e.addSeriazable(j, modRegisterInfo.ser);
                    } else {
                        e.add(obj, modRegisterInfo.hard);
                    }
                }
            }
        });

    }
}
