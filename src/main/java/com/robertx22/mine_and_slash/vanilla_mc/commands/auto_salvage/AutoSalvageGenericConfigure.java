package com.robertx22.mine_and_slash.vanilla_mc.commands.auto_salvage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleAutoSalvageRarity;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import com.robertx22.library_of_exile.command_wrapper.CommandSuggestions;
import com.robertx22.mine_and_slash.vanilla_mc.commands.suggestions.DatabaseSuggestions;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.command.EnumArgument;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AutoSalvageGenericConfigure {

    ExileRegistryType registryType;

    public AutoSalvageGenericConfigure(ExileRegistryType registryType) {
        this.registryType = registryType;
    }

    private static class EnableDisableSuggestions extends CommandSuggestions {
        @Override
        public List<String> suggestions() {
            return AutoSalvageConfigAction.getStringValues();
        }
    }

    public enum AutoSalvageConfigAction {
        ENABLE,
        DISABLE,
        CLEAR;

        public static List<String> getStringValues() {
            return Arrays.stream(AutoSalvageConfigAction.values())
                    .map(Enum::name)
                    .toList();
        }

        public String getLowercasePastTense() {
            return switch (this) {
                case CLEAR -> "cleared";
                case ENABLE -> "enabled";
                case DISABLE -> "disabled";
            };
        }
    }

    private static class RaritySuggestions extends CommandSuggestions {
        @Override
        public List<String> suggestions() {
            return ExileDB.GearRarities().getList().stream().map(x -> x.GUID()).toList();
        }
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                literal(CommandRefs.ID)
                        .then(literal("auto_salvage")
                                .then(literal("config")
                                        .then(literal(registryType.id)
                                                .then(argument("type", StringArgumentType.word())
                                                        .suggests(new DatabaseSuggestions(registryType, null))
                                                        .then(argument("action", EnumArgument.enumArgument(AutoSalvageConfigAction.class))
                                                                .suggests(new EnableDisableSuggestions())
                                                                .executes(e -> execute(e.getSource(),
                                                                        e.getSource().getPlayerOrException(),
                                                                        StringArgumentType.getString(e, "type"),
                                                                        e.getArgument("action", AutoSalvageConfigAction.class),
                                                                        null
                                                                ))
                                                                // gear types can also be configured for a single rarity, matching the salvage screen
                                                                .then(argument("rarity", StringArgumentType.word())
                                                                        .suggests(new RaritySuggestions())
                                                                        .executes(e -> execute(e.getSource(),
                                                                                e.getSource().getPlayerOrException(),
                                                                                StringArgumentType.getString(e, "type"),
                                                                                e.getArgument("action", AutoSalvageConfigAction.class),
                                                                                StringArgumentType.getString(e, "rarity")
                                                                        ))))))))
        );
    }

    private int execute(CommandSourceStack commandSource, Player player, String typeId, AutoSalvageConfigAction action, String rarityId) {

        List<String> allTypeIds = new DatabaseSuggestions(registryType, null).suggestions();

        if (!allTypeIds.contains(typeId)) {
            player.sendSystemMessage(Component.literal("The type provided: " + typeId + ", for (" + registryType.id + ") is not valid.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (rarityId != null && !ExileDB.GearRarities().isRegistered(rarityId)) {
            player.sendSystemMessage(Component.literal("The rarity provided: " + rarityId + ", is not valid.").withStyle(ChatFormatting.RED));
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

        PlayerConfigData playerConfigData = Load.player(player).config;

        if (registryType == ExileRegistryTypes.GEAR_TYPE) {
            // no SalvageType key at all here, which is exactly why the WEAPON/ARMOR mismatch can't come back
            playerConfigData.salvage.setGearTypeSalvageConfig(typeId, rarityId, action);
        } else {
            if (rarityId != null) {
                player.sendSystemMessage(Component.literal("A rarity can only be given for " + ExileRegistryTypes.GEAR_TYPE.id + ".").withStyle(ChatFormatting.RED));
                return 0;
            }
            playerConfigData.salvage.setAutoSalvageForTypeAndId(ToggleAutoSalvageRarity.SalvageType.SPELL, typeId, action);
        }

        Load.player(player).playerDataSync.setDirtyAndSync(player);

        String rarityPart = rarityId == null ? "all rarities" : rarityId;

        player.sendSystemMessage(Component.literal("Successfully updated auto_salvage settings for " + registryType.id + ": " + typeId + " (" + rarityPart + ") to " + action.getLowercasePastTense()).withStyle(ChatFormatting.GREEN));

        return 1;

    }

}
