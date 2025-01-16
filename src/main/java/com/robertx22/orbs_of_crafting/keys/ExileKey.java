package com.robertx22.orbs_of_crafting.keys;

import com.google.common.base.Preconditions;
import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.library_of_exile.recipe.RecipeGenerator;
import com.robertx22.library_of_exile.registry.ExileRegistry;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IGUID;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
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


    private boolean cancelItemCreation = false;


    public ExileKey(ExileKeyHolder holder, Info info, BiFunction<String, Info, T> sup, String id) {
        this.holder = holder;
        this.info = info;
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
        RecipeGenerator.addRecipe(SlashRef.MODID, () -> {
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


    public static <T extends ExileRegistry<T>, Info extends KeyInfo> ExileKey<T, Info> ofId(ExileKeyHolder holder, String id, Function<Info, T> sup) {
        return new ExileKey<>(holder, (Info) new IdKey(id), (s, info) -> sup.apply(info), id);
    }

    @Override
    public String GUID() {
        return id;
    }

    public void register() {
        obj = sup.apply(id, info);

        if (obj instanceof JsonExileRegistry<?> j) {
            j.addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);
        } else {
            obj.registerToExileRegistry(MMORPG.HARDCODED_REGISTRATION_INFO);
        }
    }
}
