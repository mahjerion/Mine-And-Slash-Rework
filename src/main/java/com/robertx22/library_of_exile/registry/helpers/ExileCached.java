package com.robertx22.library_of_exile.registry.helpers;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;

import java.util.function.Supplier;

// simple way to cache stuff like which item is which registry entry,
// and easy way to make it reset on database changes
public class ExileCached<T> {

    private T obj;

    private Supplier<T> sup;

    public ExileCached(Supplier<T> creator) {
        this.sup = creator;
    }

    public ExileCached<T> clearOnDatabaseChange() {
        ExileEvents.ON_REGISTER_TO_DATABASE.register(new EventConsumer<>() {
            @Override
            public void accept(ExileEvents.OnRegisterToDatabase e) {
                clear();
            }
        });
        return this;
    }

    public T get() {
        if (obj == null) {
            obj = sup.get();
        }
        return obj;
    }

    public void clear() {
        obj = null;
    }
}
