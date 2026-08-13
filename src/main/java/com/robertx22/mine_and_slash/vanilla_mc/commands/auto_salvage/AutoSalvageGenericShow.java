package com.robertx22.mine_and_slash.vanilla_mc.commands.auto_salvage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.database.data.support_gem.SupportGem;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleAutoSalvageRarity;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Objects;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AutoSalvageGenericShow {

    ExileRegistryType registryType;

    public AutoSalvageGenericShow(ExileRegistryType registryType) {
        this.registryType = registryType;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                literal(CommandRefs.ID)
                        .then(literal("auto_salvage")
                                .then(literal("show")
                                        .then(literal(registryType.id)
                                                .executes(e -> execute(e.getSource(), e.getSource().getPlayerOrException(), null))
                                                .then(argument("search_query", StringArgumentType.word())
                                                        .executes(e -> execute(e.getSource(), e.getSource().getPlayerOrException(), StringArgumentType.getString(e, "search_query")))
                                                ))))
        );
    }

    private int execute(CommandSourceStack commandSource, Player player, String searchQuery) {


        if (Objects.isNull(player)) {
            try {
                player = commandSource.getPlayerOrException();
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                return 0;
            }
        }

        PlayerConfigData playerConfigData = Load.player(player).config;

        if (registryType == ExileRegistryTypes.GEAR_TYPE) {
            return showGearTypes(player, playerConfigData, searchQuery);
        }

        HashMap<String, Boolean> configuredMap = playerConfigData.salvage.getConfiguredMapForSalvageType(ToggleAutoSalvageRarity.SalvageType.SPELL);

        if (configuredMap.isEmpty()) {
            player.sendSystemMessage(Component.literal("There are no currently configured options for " + registryType.id + " items.").withStyle(ChatFormatting.GRAY));
            return 1;
        }

        if (searchQuery == null) {
            player.sendSystemMessage(Component.literal("--- Listing all configured " + registryType.id + " ids ---"));
        } else {
            player.sendSystemMessage(Component.literal("--- Listing configured " + registryType.id + " ids matching: " + searchQuery + " ---"));
        }

        for (String id : configuredMap.keySet()) {
            var enabled = configuredMap.get(id);
            if (registryType == ExileRegistryTypes.SUPPORT_GEM) {
                SupportGem gem = ExileDB.SupportGems().get(id);
                if (searchQuery == null || gem.id.toLowerCase().contains(searchQuery.toLowerCase()) || gem.locName().getString().toLowerCase().contains(searchQuery.toLowerCase())) {
                    player.sendSystemMessage(Component.literal("[" + gem.id + "] " + gem.locName().getString() + " [").append(enabledText(enabled)).append(Component.literal("]").withStyle(ChatFormatting.WHITE)));
                }
            } else {
                if (searchQuery == null || id.toLowerCase().contains(searchQuery.toLowerCase())) {
                    player.sendSystemMessage(Component.literal(id + " [").append(enabledText(enabled)).append(Component.literal("]").withStyle(ChatFormatting.WHITE)));
                }
            }
        }

        player.sendSystemMessage(Component.literal("---~---"));

        return 1;

    }

    private static Component enabledText(boolean enabled) {
        return enabled
                ? Component.literal("ENABLED").withStyle(ChatFormatting.GREEN)
                : Component.literal("DISABLED").withStyle(ChatFormatting.RED);
    }

    // gear types are configured per rarity, so print one line per gear type with its rarity breakdown
    private int showGearTypes(Player player, PlayerConfigData playerConfigData, String searchQuery) {

        var all = playerConfigData.salvage.getGtMap();

        if (all.isEmpty()) {
            player.sendSystemMessage(Component.literal("There are no currently configured options for " + registryType.id + " items.").withStyle(ChatFormatting.GRAY));
            return 1;
        }

        if (searchQuery == null) {
            player.sendSystemMessage(Component.literal("--- Listing all configured " + registryType.id + " ids ---"));
        } else {
            player.sendSystemMessage(Component.literal("--- Listing configured " + registryType.id + " ids matching: " + searchQuery + " ---"));
        }

        for (String id : all.keySet().stream().sorted().toList()) {

            if (searchQuery != null && !id.toLowerCase().contains(searchQuery.toLowerCase())) {
                continue;
            }

            var rarities = all.get(id);

            if (rarities == null || rarities.isEmpty()) {
                continue;
            }

            var line = Component.literal(id + ": ").withStyle(ChatFormatting.WHITE);

            boolean first = true;
            for (String rar : rarities.keySet().stream().sorted().toList()) {
                if (!first) {
                    line.append(Component.literal(", ").withStyle(ChatFormatting.WHITE));
                }
                line.append(Component.literal(rar + " ").withStyle(ChatFormatting.GRAY)).append(enabledText(rarities.get(rar)));
                first = false;
            }

            player.sendSystemMessage(line);
        }

        player.sendSystemMessage(Component.literal("---~---"));

        return 1;
    }

}
