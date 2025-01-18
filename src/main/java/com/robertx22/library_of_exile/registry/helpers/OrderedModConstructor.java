package com.robertx22.library_of_exile.registry.helpers;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.registry.ExileRegistryEvent;
import com.robertx22.library_of_exile.registry.ExileRegistryEventClass;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.HashMap;
import java.util.List;

// used to know how to register stuff properly without headaches
public abstract class OrderedModConstructor {

    public static HashMap<String, OrderedModConstructor> all = new HashMap<>();

    String modid;

    public OrderedModConstructor(String modid, IEventBus modbus) {
        this.modid = modid;
        registerDeferredContainers(modbus);
        registerDeferredEntries();
        registerDatabases();

        final boolean[] done = {false};
        ExileEvents.EXILE_REGISTRY_GATHER.register(new EventConsumer<ExileRegistryEvent>() {
            @Override
            public void accept(ExileRegistryEvent e) {
                if (!done[0]) {
                    registerDatabaseEntries();
                    done[0] = true;
                }
            }
        });
        
        for (ExileRegistryEventClass event : this.getRegisterEvents()) {
            event.register();
        }

        //registerDatabaseEntries();

        for (ExileKeyHolder holder : getAllKeyHolders()) {
            holder.init();
        }


        all.put(modid, this);
    }

    public abstract List<ExileRegistryEventClass> getRegisterEvents();

    public abstract List<ExileKeyHolder> getAllKeyHolders();

    public abstract void registerDeferredContainers(IEventBus bus);

    public abstract void registerDeferredEntries();

    public abstract void registerDatabases();

    public abstract void registerDatabaseEntries();

}
