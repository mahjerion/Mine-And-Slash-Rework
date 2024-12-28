package com.robertx22.library_of_exile.deferred;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class RegObj<T> {

    public RegObj(RegistryObject<T> obj) {
        this.obj = obj;
    }

    private final RegistryObject<T> obj;

    //DO NOT CALL THIS BEFORE THE FORGE'S REGISTRY EVENT FOR THIS TYPE HAS BEEN CALLED
    public T get() {
        return obj.get();
    }

    public RegistryObject<T> getRegistryObject() {
        return obj;
    }

    public static <I> RegObj<I> register(String id, Supplier<I> sup, DeferredRegister<I> register) {
        RegistryObject<I> reg = register.register(id, sup);
        RegObj<I> wrapper = new RegObj<I>(reg);
        return wrapper;
    }

}
