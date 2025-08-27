package com.robertx22.mine_and_slash.vanilla_mc.commands.suggestions;

import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.library_of_exile.command_wrapper.CommandSuggestions;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryContainer;
import com.robertx22.library_of_exile.registry.IGUID;

import java.util.List;
import java.util.stream.Collectors;

public class MapTypeSuggestions extends CommandSuggestions {
    @Override
    public List<String> suggestions() {
        ExileRegistryContainer<? extends IGUID> reg = Database.getRegistry(DungeonDatabase.DUNGEON);
        var list = reg.getList().stream().map(x -> x.GUID()).collect(Collectors.toList());
        list.add("random");
        return list;
    }
}

