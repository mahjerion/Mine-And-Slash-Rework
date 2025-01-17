package com.robertx22.orbs_of_crafting.keys;

import com.robertx22.library_of_exile.registry.ExileRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ExileKeyMap<T extends ExileRegistry<T>, Info extends KeyInfo> {

    String id_prefix;
    public Function<Info, String> idMaker = x -> id_prefix + "_" + x.GUID();
    public List<Info> all = new ArrayList<>();
    public HashMap<Info, ExileKey<T, Info>> map = new HashMap<>();

    ExileKeyHolder holder;

    public String getId(Info info) {
        return idMaker.apply(info);
    }

    public ExileKey<T, Info> get(Info info) {
        return map.get(info);
    }


    public ExileKeyMap(ExileKeyHolder<T> holder, String id_prefix) {
        this.id_prefix = id_prefix;
        this.holder = holder;
    }

    public ExileKeyMap<T, Info> ofList(List<Info> all) {
        this.all = all;
        return this;
    }

    public <R> ExileKeyMap<T, Info> ofList(List<R> all, Function<R, Info> f) {
        this.all = all.stream().map(f).collect(Collectors.toList());
        return this;
    }


    private void b(BiFunction<String, Info, T> classMaker) {
        for (Info info : all) {
            var key = new ExileKey<>(holder, info, classMaker, idMaker.apply(info));
            map.put(info, key);
        }
    }

    public ExileKeyMap<T, Info> build(BiFunction<String, Info, T> classMaker) {
        b(classMaker);
        return this;
    }
}
