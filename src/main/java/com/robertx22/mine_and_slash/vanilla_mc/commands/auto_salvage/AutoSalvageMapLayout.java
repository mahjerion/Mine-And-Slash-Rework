package com.robertx22.mine_and_slash.vanilla_mc.commands.auto_salvage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import com.robertx22.mine_and_slash.vanilla_mc.commands.suggestions.DatabaseSuggestions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.command.EnumArgument;

import java.util.List;
import java.util.Objects;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

// the command form of the Map Layouts page on the salvage screen
public class AutoSalvageMapLayout {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                literal(CommandRefs.ID)
                        .then(literal("auto_salvage")
                                .then(literal("map_layout")
                                        .then(argument("layout", StringArgumentType.word())
                                                .suggests(new DatabaseSuggestions(DungeonDatabase.DUNGEON, null))
                                                .then(argument("action", EnumArgument.enumArgument(AutoSalvageGenericConfigure.AutoSalvageConfigAction.class))
                                                        .executes(e -> execute(e.getSource(),
                                                                e.getSource().getPlayerOrException(),
                                                                StringArgumentType.getString(e, "layout"),
                                                                e.getArgument("action", AutoSalvageGenericConfigure.AutoSalvageConfigAction.class)
                                                        )))))));
    }

    private static int execute(CommandSourceStack commandSource, Player player, String layoutId, AutoSalvageGenericConfigure.AutoSalvageConfigAction action) {

        List<String> all = new DatabaseSuggestions(DungeonDatabase.DUNGEON, null).suggestions();

        if (!all.contains(layoutId)) {
            player.sendSystemMessage(Component.literal("The map layout provided: " + layoutId + ", is not valid.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (Objects.isNull(player)) {
            try {
                player = commandSource.getPlayerOrException();
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                return 0;
            }
        }

        PlayerConfigData config = Load.player(player).config;

        // CLEAR and DISABLE both mean the same thing here: no entry, aka keep
        config.salvage.setMapLayoutFilter(layoutId, action == AutoSalvageGenericConfigure.AutoSalvageConfigAction.ENABLE);

        Load.player(player).playerDataSync.setDirtyAndSync(player);

        if (config.salvage.isMapLayoutFiltered(layoutId)) {
            player.sendSystemMessage(Component.literal("Maps with the " + layoutId + " layout will now be salvaged on pickup at any rarity.").withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.literal("Maps with the " + layoutId + " layout are no longer filtered out.").withStyle(ChatFormatting.GREEN));
        }

        return 1;
    }
}
